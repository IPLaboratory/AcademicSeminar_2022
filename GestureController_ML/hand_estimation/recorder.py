class Recorder:
    def __init__(self):
        self.data = []
        self.skip = 1
        self.skip_count = 0

    def record(self, landmarks):
        if self.skip_count < self.skip:
            self.skip_count += 1
            return

        self.skip_count = 0

        value = ''
        positions = [(landmark.x, landmark.y, landmark.z) for landmark in landmarks.landmark]
        for (x, y, z) in positions:
            value = f'{value} {x} {y} {z}'
        value = value.strip()
        self.data.append(value)
        print(value)

    def save(self):
        if self.data:
            with open('---record---.txt', 'a+') as file:
                for value in self.data:
                    file.write(value + '\n')
            self.data = []
