import os
import shutil
import random
import cv2
import tensorflow as tf
from hand_estimation.estimate import Estimater


class Gym:
    def __init__(self, train_image_path, train_data_path, output_model_path, output_label_path):
        self.train_image_path = train_image_path
        self.train_data_path = train_data_path
        self.output_model_path = output_model_path
        self.output_label_path = output_label_path
        self.epochs = 50
        self.classes = 0
        self.class_names = []
        self.estimater = Estimater()


    def train(self, model_updated_flag):
        self.__convert_to_landmarks(self.train_image_path)
        self.classes = len(next(os.walk(self.train_data_path))[2])

        x = []
        y = []
        for root, _, files in os.walk(self.train_data_path):
            for i, file in enumerate(files):
                self.class_names.append(os.path.splitext(file)[0])
                with open(os.path.join(root, file), 'r') as file:
                    x_data = [list(map(float, line.split())) for line in file.readlines()]
                    y_data = [0] * self.classes
                    y_data[i] = 1
                    y += [y_data] * len(x_data)
                x += x_data

        shuffled_xy = list(zip(x, y))
        random.shuffle(shuffled_xy)
        x = [x for (x, _) in shuffled_xy]
        y = [y for (_, y) in shuffled_xy]

        self.__train_by_tensorflow(x, y)

        model_updated_flag.value = 3


    def __convert_to_landmarks(self, train_image_path):
        if not os.path.exists(train_image_path):
            return

        for root, _, files in os.walk(train_image_path):
            for file in files:
                image = cv2.imread(os.path.join(root, file))
                if image is None:
                    continue
                result = self.estimater.predict(image)
                if result.multi_hand_world_landmarks:
                    with open(f'{os.path.join(self.train_data_path, os.path.basename(root))}.txt', 'a+') as train_data_file:
                        positions = [sum([[landmark.x, landmark.y, landmark.z] for landmark in landmarks.landmark], start=[]) for landmarks in result.multi_hand_world_landmarks][0]
                        train_data_file.write(' '.join(map(str, positions)) + '\n')

        shutil.rmtree(train_image_path)
        

    def __train_by_tensorflow(self, x, y):
        model = tf.keras.Sequential([
            tf.keras.layers.Dense(units=512, input_dim=63, activation='relu'),
            tf.keras.layers.Dense(units=1024, activation='relu'),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(units=1024, activation='relu'),
            tf.keras.layers.Dropout(0.2),
            tf.keras.layers.Dense(units=1024, activation='relu'),
            tf.keras.layers.Dense(units=512, activation='relu'),
            tf.keras.layers.Dense(units=self.classes, activation='softmax'),
        ])

        model.compile(
            optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
            loss='mean_squared_error',
            metrics=['accuracy'],
        )

        model.fit(x, y, epochs=self.epochs, batch_size=1024, verbose=0)

        model.save(self.output_model_path)

        with open(self.output_label_path, 'w+') as file:
            for class_name in self.class_names:
                file.write(class_name + '\n')
