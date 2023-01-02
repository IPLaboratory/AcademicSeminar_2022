import os
import base64
import asyncio
import socketio
import cv2
from gym import Gym
from hand_estimation import draw
from hand_estimation import gesture
from hand_estimation.recorder import Recorder
from hand_estimation.gesture import Hand, Compare
from hand_estimation.estimate import Estimater
from hand_estimation.singleton import TimeManager, Observer
from multiprocessing.dummy import Process, Value

start_record = False
recorder = Recorder()
sio = socketio.AsyncClient()
model_updated_flag = Value('i', 3)

left_hand_gesture = 'five'

pre_trained_device_id = {
    'one': 0,
    'two': 1,
    'three': 2,
    'four': 3,
}

pre_trained_gestures = [
    'zero',
    'one',
    'two',
    'two_range',
    'three',
    'four',
    'five',
    'up',
    'down',
    'dumpling',
]

pre_trained_gesture_id = {
    'up': 1,
    'down': 2,
    'one_range_decrease': 3,
    'one_range_increase': 4,
    'two_range_decrease': 5,
    'two_range_increase': 6,
    'dumpling': 7,
}

@sio.event
async def connect():
    print('connection established')
    await sio.emit('client registration', {'device': 'ml'})


@sio.on('add gesture')
async def add_gesture(data):
    total_count, current_count, gesture_id, frame = data['all'], data['count'], data['gesture_id'], data['frame']
    print(f'{current_count / total_count * 100.0:.1f}% downloaded from server!')

    os.makedirs(f'./gym/train_image/{gesture_id}', exist_ok=True)
    with open(f'./gym/train_image/{gesture_id}/video.txt', 'a+') as file:
        file.write(frame)

    if current_count == total_count:
        print('Decoding data...')

        def decode_video(flag):
            data = None
            with open(f'./gym/train_image/{gesture_id}/video.txt', 'r') as file:
                data = file.read().strip()
                
            with open(f'./gym/train_image/{gesture_id}/video.mp4', 'wb+') as file:
                file.write(base64.b64decode(data))

            video = cv2.VideoCapture(f'./gym/train_image/{gesture_id}/video.mp4')
            sample_count = 50
            step = int(video.get(cv2.CAP_PROP_FRAME_COUNT) / sample_count)
            i = 0
            while video.isOpened():
                success, image = video.read()
                if not success:
                    break

                if int(video.get(cv2.CAP_PROP_POS_FRAMES)) % step == 0:
                    image = cv2.rotate(image, cv2.ROTATE_180)
                    cv2.imwrite(f'./gym/train_image/{gesture_id}/{i}.png', image)
                    i += 1

            os.remove(f'./gym/train_image/{gesture_id}/video.txt')
            os.remove(f'./gym/train_image/{gesture_id}/video.mp4')

            flag.value = 1

        video_decode_process = Process(target=decode_video, args=(model_updated_flag, ))
        video_decode_process.start()

        pre_trained_device_id[f'{gesture_id}'] = gesture_id


@sio.event
async def disconnect():
    print('disconnected from server')


async def process(image, result):
    # INFO: 출품시에는 recorder.py test.py predict.py 및 관련 코드 지워도 됨 
    global left_hand_gesture

    gestures = gesture.static(result.multi_hand_world_landmarks)

    for index, landmarks in enumerate(result.multi_hand_landmarks):
        label = f'Index{index}: Side={result.multi_handedness[index].classification[0].label}, Gesture={gestures[index]}'
        draw.box(image, landmarks, label)
        draw.hand(image, landmarks)

        if result.multi_handedness[index].classification[0].label == 'Left':
            last, current = Observer.get_instance().on_change('left_hand_changed', gestures[index])
            if last != current:
                TimeManager.get_instance().remove_timer('one_range')
                TimeManager.get_instance().remove_timer('two_range')
                TimeManager.get_instance().remove_timer('up')
                TimeManager.get_instance().remove_timer('down')
                left_hand_gesture = gestures[index]
        if result.multi_handedness[index].classification[0].label == 'Right':
            last, current = Observer.get_instance().on_change('right_hand_changed', gestures[index])
            if last != current:
                TimeManager.get_instance().remove_timer('one_range')
                TimeManager.get_instance().remove_timer('two_range')
                TimeManager.get_instance().remove_timer('up')
                TimeManager.get_instance().remove_timer('down')

        if left_hand_gesture not in pre_trained_device_id.keys():
            continue

        if gestures[index] == 'one' and result.multi_handedness[index].classification[0].label == 'Right':
            comparation = gesture.range(landmarks, 'one_range', Hand.INDEX_FINGER_TIP).absolute_by(image).comparation
            if comparation == Compare.DECREASE:
                print(pre_trained_device_id[left_hand_gesture], pre_trained_gesture_id['one_range_decrease'])
                await sio.emit('get gesture', {
                    'did': pre_trained_device_id[left_hand_gesture],
                    'gid': pre_trained_gesture_id['one_range_decrease'],
                })
            elif comparation == Compare.INCREASE:
                print(pre_trained_device_id[left_hand_gesture], pre_trained_gesture_id['one_range_increase'])
                await sio.emit('get gesture', {
                    'did': pre_trained_device_id[left_hand_gesture],
                    'gid': pre_trained_gesture_id['one_range_increase'],
                })

        elif gestures[index] == 'two_range' and result.multi_handedness[index].classification[0].label == 'Right':
            comparation = gesture.range(landmarks, 'two_range', Hand.INDEX_FINGER_TIP).absolute_by(image).comparation
            if comparation == Compare.DECREASE:
                print(pre_trained_device_id[left_hand_gesture], pre_trained_gesture_id['two_range_decrease'])
                await sio.emit('get gesture', {
                    'did': pre_trained_device_id[left_hand_gesture],
                    'gid': pre_trained_gesture_id['two_range_decrease'],
                })
            elif comparation == Compare.INCREASE:
                print(pre_trained_device_id[left_hand_gesture], pre_trained_gesture_id['two_range_increase'])
                await sio.emit('get gesture', {
                    'did': pre_trained_device_id[left_hand_gesture],
                    'gid': pre_trained_gesture_id['two_range_increase'],
                })
                
        elif gestures[index] == 'up' and result.multi_handedness[index].classification[0].label == 'Right':
            TimeManager.get_instance().add_timer('up', 1, True)
            if TimeManager.get_instance().pick_result('up'):
                TimeManager.get_instance().remove_timer('up')
                print(pre_trained_device_id[left_hand_gesture], pre_trained_gesture_id['up'])
                await sio.emit('get gesture', {
                    'did': pre_trained_device_id[left_hand_gesture],
                    'gid': pre_trained_gesture_id['up'],
                })
            
        elif gestures[index] == 'down' and result.multi_handedness[index].classification[0].label == 'Right':
            TimeManager.get_instance().add_timer('down', 1, True)
            if TimeManager.get_instance().pick_result('down'):
                TimeManager.get_instance().remove_timer('down')
                print(pre_trained_device_id[left_hand_gesture], pre_trained_gesture_id['down'])
                await sio.emit('get gesture', {
                    'did': pre_trained_device_id[left_hand_gesture],
                    'gid': pre_trained_gesture_id['down'],
                })

        elif gestures[index] == 'dumpling' and result.multi_handedness[index].classification[0].label == 'Right':
            TimeManager.get_instance().add_timer('dumpling', 1, True)
            if TimeManager.get_instance().pick_result('dumpling'):
                TimeManager.get_instance().remove_timer('dumpling')
                print(pre_trained_device_id[left_hand_gesture], pre_trained_gesture_id['dumpling'])
                await sio.emit('get gesture', {
                    'did': pre_trained_device_id[left_hand_gesture],
                    'gid': pre_trained_gesture_id['dumpling'],
                })

        elif gestures[index] not in pre_trained_gestures and result.multi_handedness[index].classification[0].label == 'Right':
            TimeManager.get_instance().add_timer(f'{gestures[index]}', 1, True)
            if TimeManager.get_instance().pick_result(f'{gestures[index]}'):
                TimeManager.get_instance().remove_timer(f'{gestures[index]}')
                print(pre_trained_device_id[left_hand_gesture], int(gestures[index]))
                await sio.emit('get gesture', {
                    'did': pre_trained_device_id[left_hand_gesture],
                    'gid': int(gestures[index]),
                })


async def main():
    global start_record
    # await sio.connect('http://192.168.0.7:8080')
    await sio.connect('http://220.69.208.237:8080')

    window_name = 'IPL Gesture Controller'

    with Estimater() as estimater:
        while True:
            if model_updated_flag.value == 1:
                print('Start model update!')
                model_update_process = Process(target=Gym('./gym/train_image', './gym/train_data', './gym/trained_model_new.h5', './gym/trained_label_new.txt').train, args=(model_updated_flag,))
                model_update_process.start()
                model_updated_flag.value = 2
            elif model_updated_flag.value == 3:
                gesture.load_model()
                await sio.emit('get mlResult', { 'result': True }, namespace='/')
                print('Model updated successfully!')
                model_updated_flag.value = 0

            image, result = estimater.process()
            TimeManager.get_instance().update()

            if gesture.enable(result):
                await process(image, result)

            if result.multi_hand_world_landmarks:
                for landmarks in result.multi_hand_world_landmarks:
                    if start_record:
                        recorder.record(landmarks)
                        draw.text(image, 'Recording...', (50, 20), (0, 0, 255), 1)
                    else:
                        recorder.save()
            await sio.sleep()

            cv2.imshow(window_name, image)
            
            key = cv2.waitKey(1)
            if key & 0xFF == 27:
                break
            elif key & 0xFF == 32:
                start_record = not start_record

    await sio.disconnect()


if __name__ == '__main__':
    asyncio.run(main())
