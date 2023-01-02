import os
from enum import Enum
import numpy as np
import tensorflow as tf
from hand_estimation.singleton import TimeManager, Observer


model = None
encoded_outputs = None


def load_model():
    global model
    global encoded_outputs

    if os.path.exists('./gym/trained_model_new.h5'):
        os.remove('./gym/trained_model.h5')
        os.rename('./gym/trained_model_new.h5', './gym/trained_model.h5')

    if os.path.exists('./gym/trained_label_new.txt'):
        os.remove('./gym/trained_label.txt')
        os.rename('./gym/trained_label_new.txt', './gym/trained_label.txt')

    model = tf.keras.models.load_model('./gym/trained_model.h5')
    with open('./gym/trained_label.txt', 'r') as file:
        encoded_outputs = file.readlines()
        encoded_outputs = [encoded_output.strip() for encoded_output in encoded_outputs]


class Hand(Enum):
    WRIST = 0
    THUMB_CMC = 1
    THUMB_MCP = 2
    THUMB_IP = 3
    THUMB_TIP = 4
    INDEX_FINGER_MCP = 5
    INDEX_FINGER_PIP = 6
    INDEX_FINGER_DIP = 7
    INDEX_FINGER_TIP = 8
    MIDDLE_FINGER_MCP = 9
    MIDDLE_FINGER_PIP = 10
    MIDDLE_FINGER_DIP = 11
    MIDDLE_FINGER_TIP = 12
    RING_FINGER_MCP = 13
    RING_FINGER_PIP = 14
    RING_FINGER_DIP = 15
    RING_FINGER_TIP = 16
    PINKY_MCP = 17
    PINKY_PIP = 18
    PINKY_DIP = 19
    PINKYTIP = 20


class Compare(Enum):
    INCREASE = 0
    DECREASE = 1
    EQUAL = 2


class Point:
    def __init__(self, x, y, z):
        self.x = x
        self.y = y
        self.z = z


class Pinch:
    def __init__(self, src, dst, ref_src, ref_dst):
        self.src = src
        self.dst = dst
        self.ref_src = ref_src
        self.ref_dst = ref_dst
        self.distance = np.sqrt(
            pow(abs(src.x - dst.x), 2)
            + pow(abs(src.y - dst.y), 2)
            + pow(abs(src.z - dst.z), 2)
        ) * 1 / np.sqrt(
            pow(abs(ref_src.x - ref_dst.x), 2)
            + pow(abs(ref_src.y - ref_dst.y), 2)
            + pow(abs(ref_src.z - ref_dst.z), 2)
        )

    def absolute_by(self, image):
        h, w, _ = image.shape
        self.distance = np.sqrt(
            pow(abs(self.src.x * w - self.dst.x * w), 2)
            + pow(abs(self.src.y * h - self.dst.y * h), 2)
            + pow(abs(self.src.z * h - self.dst.z * h), 2)
        ) * 25 / np.sqrt(
            pow(abs(self.ref_src.x * w - self.ref_dst.x * w), 2)
            + pow(abs(self.ref_src.y * h - self.ref_dst.y * h), 2)
            + pow(abs(self.ref_src.z * h - self.ref_dst.z * h), 2)
        )
        return self


def pinch(landmarks, src_joint, dst_joint):
    return Pinch(
        Point(landmarks.landmark[src_joint.value].x, landmarks.landmark[src_joint.value].y, landmarks.landmark[src_joint.value].z),
        Point(landmarks.landmark[dst_joint.value].x, landmarks.landmark[dst_joint.value].y, landmarks.landmark[dst_joint.value].z),
        Point(landmarks.landmark[Hand.INDEX_FINGER_DIP.value].x, landmarks.landmark[Hand.INDEX_FINGER_DIP.value].y, landmarks.landmark[Hand.INDEX_FINGER_DIP.value].z),
        Point(landmarks.landmark[Hand.INDEX_FINGER_TIP.value].x, landmarks.landmark[Hand.INDEX_FINGER_TIP.value].y, landmarks.landmark[Hand.INDEX_FINGER_TIP.value].z),
    )


class Range:
    def __init__(self, id, src, dst):
        self.id = id
        self.src = src
        self.dst = dst
        self.distance = dst.x - src.x
        self.section = self.distance / 0.05
        self.comparation = Compare.EQUAL
    
    def absolute_by(self, image):
        h, w, c = image.shape
        self.distance = self.dst.x * w - self.src.x * w
        self.section = int(self.distance / 50)
        last, current = Observer.get_instance().on_change(id, self.section)
        if current - last > 0:
            self.comparation = Compare.INCREASE
        elif current - last < 0:
            self.comparation = Compare.DECREASE
        else:
            self.comparation = Compare.EQUAL
        return self


def range(landmarks, id, dst_joint):
    TimeManager.get_instance().add_timer(id, 1, (landmarks.landmark[Hand.INDEX_FINGER_TIP.value].x, ))
    results = TimeManager.get_instance().pick_result(id)
    
    instance = Range(id, Point(landmarks.landmark[dst_joint.value].x, 0 ,0), Point(landmarks.landmark[dst_joint.value].x, 0, 0))
    if results:
        while not results.empty():
            instance = Range(id, Point(results.get()[0], 0 ,0), Point(landmarks.landmark[dst_joint.value].x, 0, 0))

    return instance


def static(multi_hand_world_landmarks):
    inputs = [sum([[landmark.x, landmark.y, landmark.z] for landmark in landmarks.landmark], start=[]) for landmarks in multi_hand_world_landmarks]
    inputs = np.array(inputs)
    gestures = model(inputs, training=False)
    gestures = [np.argmax(result) for result in gestures]
    gestures = list(map(lambda x: encoded_outputs[x], gestures))
    return gestures


class Enable:
    def __init__(self):
        self.gesture_enabled = False

enable_instance = Enable()

def enable(result):
    if not result.multi_hand_landmarks or not result.multi_hand_world_landmarks:
        return False

    if len(result.multi_handedness) != 2:
        return False

    handsides = [handedness.classification[0].label for handedness in result.multi_handedness]
    if 'Left' not in handsides or 'Right' not in handsides:
        return False

    gestures = static(result.multi_hand_world_landmarks)

    if gestures[0] == 'five' and gestures[1] == 'five':
        enable_instance.gesture_enabled = True
    elif gestures[0] == 'zero' and gestures[1] == 'zero':
        enable_instance.gesture_enabled = False

    if not enable_instance.gesture_enabled:
        return False

    return True
