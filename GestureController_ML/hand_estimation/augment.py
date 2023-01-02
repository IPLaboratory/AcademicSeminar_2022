from os.path import basename, splitext, join
from shutil import copyfile
import cv2
import albumentations


class ImageAugmentationBuilder:
    def __init__(
        self,
        image_paths: list[str] = None,
        keypoint_paths: list[str] = None,
        image_output_path: str = '',
        copy_origin_image: bool = False,
        keypoint_output_path: str = '',
        copy_origin_keypoint: bool = False,
    ):
        self.generator = []
        self.image_paths = image_paths
        self.keypoint_paths = keypoint_paths
        self.image_output_path = image_output_path
        self.copy_origin_image = copy_origin_image
        self.keypoint_output_path = keypoint_output_path
        self.copy_origin_keypoint = copy_origin_keypoint

    def images(self, paths: list[str]):
        self.image_paths = paths
        return self

    def keypoints(self, paths: list[str]):
        self.keypoint_paths = paths
        return self

    def image_output(self, path: str, copy_origin: bool = False):
        self.image_output_path = path
        self.copy_origin_image = copy_origin
        return self

    def keypoint_output(self, path: str, copy_origin: bool = False):
        self.keypoint_output_path = path
        self.copy_origin_keypoint = copy_origin
        return self

    def generate(self, postfix: str, commands: list):
        self.generator.append((postfix, commands))
        return self

    def build(self):
        # for image_path, keypoint_path in zip(self.image_paths, self.keypoint_paths):
        for image_path in self.image_paths:
            image = cv2.imread(image_path)

            if self.image_output_path and self.copy_origin_image:
                filename = basename(image_path)
                copyfile(image_path, join(self.image_output_path, filename))

            # class_labels: list[str] = []
            # keypoints: list[float] = []
            # with open(keypoint_path) as file:
            #     for keypoint in file:
            #         class_labels.append(keypoint.split()[0])
            #         keypoints.append([float(k) for k in keypoint.split()[1:]])

            # if self.keypoint_output_path and self.copy_origin_keypoint:
            #     filename = basename(keypoint_path)
            #     copyfile(keypoint_path, join(self.keypoint_output_path, filename))

            for postfix, commands in self.generator:
                transform = albumentations.Compose(
                    commands,
                    # keypoint_params=albumentations.KeypointParams(
                    #     format='xy',
                    #     label_fields=['class_labels'],
                    # ),
                )

                # transformed = transform(image=image, keypoints=keypoints, class_labels=class_labels)
                transformed = transform(image=image)
                transformed_image = transformed['image']
                # transformed_keypoints = transformed['keypoints']
                # transformed_keypoint_labels = transformed['class_labels']

                filename = basename(image_path) if self.image_output_path else image_path
                filename, extension = splitext(filename)
                cv2.imwrite(join(self.image_output_path, f'{filename}_{postfix}{extension}'), transformed_image)

                # filename = basename(keypoint_path) if self.keypoint_output_path else keypoint_path
                # filename, extension = splitext(filename)
                # with open(join(self.keypoint_output_path, f'{filename}_{postfix}{extension}'), 'w') as file:
                #     for transformed_keypoint_label, transformed_keypoint in zip(transformed_keypoint_labels, transformed_keypoints):
                #         file.write(f'{transformed_keypoint_label} {transformed_keypoint[0]} {transformed_keypoint[1]}\n')
