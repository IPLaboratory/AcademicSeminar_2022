import cv2
import mediapipe as mp


class Estimater:
    def __init__(self):
        self.capture = None
        self.hands = mp.solutions.hands.Hands(
            model_complexity=1,
            max_num_hands=2,
            min_detection_confidence=0.9,
            min_tracking_confidence=0.9,
        )

    def process(self):
        if self.capture.isOpened():
            success, image = self.capture.read()
            if success:
                image.flags.writeable = False
                image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
                image = cv2.flip(image, 1)
                result = self.hands.process(image)
                image.flags.writeable = True
                image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
                return image, result
        return None

    def predict(self, image):
        image.flags.writeable = False
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        image = cv2.flip(image, 1)
        result = self.hands.process(image)
        image.flags.writeable = True
        return result

    def release(self):
        self.capture.release()
        self.hands.close()

    def __enter__(self):
        self.capture = cv2.VideoCapture(0)
        return self

    def __exit__(self, type, value, traceback):
        self.release()
