import cv2
import mediapipe as mp


class Bound:
    def __init__(self, x=0, y=0, w=0, h=0):
        self.x = x
        self.y = y
        self.w = w
        self.h = h

    def absolute_by(self, image):
        h, w, _ = image.shape
        self.x = self.x * w
        self.y = self.y * h
        self.w = self.w * w
        self.h = self.h * h
        return self

    def __str__(self):
        return f'({self.x}, {self.y}, {self.w}, {self.h})'


def box(image, landmarks, label=None):
    x_positions = [landmark.x for landmark in landmarks.landmark]
    y_positions = [landmark.y for landmark in landmarks.landmark]
    boundingbox = Bound(
        min(x_positions),
        min(y_positions),
        max(x_positions),
        max(y_positions),
    ).absolute_by(image)

    color = (255, 0, 0)
    image = cv2.rectangle(
        image,
        list(map(int, (boundingbox.x, boundingbox.y))),
        list(map(int, (boundingbox.w, boundingbox.h))),
        color,
        1,
    )
    if label:
        image = cv2.putText(
            image,
            label,
            list(map(int, (boundingbox.x, boundingbox.y - 5))),
            cv2.FONT_HERSHEY_COMPLEX_SMALL,
            1,
            color,
            1,
        )
    return image


def text(image, label, position, color, thickness):
    image = cv2.putText(image, label, position, cv2.FONT_HERSHEY_COMPLEX_SMALL, 1, color, thickness)
    return image


def hand(image, landmarks):
    mp.solutions.drawing_utils.draw_landmarks(
        image,
        landmarks,
        mp.solutions.hands.HAND_CONNECTIONS,
        mp.solutions.drawing_styles.get_default_hand_landmarks_style(),
        mp.solutions.drawing_styles.get_default_hand_connections_style(),
    )
    return image
