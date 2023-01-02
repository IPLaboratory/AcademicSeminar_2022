import cv2
import draw
import gesture
from recorder import Recorder
from gesture import Hand
from estimate import Estimater


start_record = False
recorder = Recorder()

def process(image, result):
    if not result.multi_hand_landmarks:
        return

    positions = [sum([[landmark.x, landmark.y, landmark.z] for landmark in landmarks.landmark], start=[]) for landmarks in result.multi_hand_world_landmarks]
    gestures = gesture.static(positions)

    for index, landmarks in enumerate(result.multi_hand_landmarks):
        bbox = gesture.bound(landmarks).absolute_by(image)
        distance = gesture.pinch(landmarks, Hand.THUMB_TIP, Hand.INDEX_FINGER_TIP).absolute_by(image).distance
        handside_label = f'Index{index}: Side={result.multi_handedness[index].classification[0].label}, Gesture={gestures[index]}, Distance={distance}'
        draw.box(image, bbox, (0, 0, 255), 1, handside_label)

        draw.hand(image, landmarks)

    for landmarks in result.multi_hand_world_landmarks:
        if start_record:
            recorder.record(landmarks)
            draw.text(image, 'Recording...', (50, 20), (0, 0, 255), 1)
        else:
            recorder.save()


with Estimater() as estimater:
    while True:
        image, result = estimater.process()
        process(image, result)
        cv2.imshow('MediaPipe Hands', image)

        key = cv2.waitKey(5)
        if key & 0xFF == 27:
            break
        elif key & 0xFF == 32:
            start_record = not start_record
