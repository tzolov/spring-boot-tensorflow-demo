package org.springframework.cloud.stream.app.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.aggregate.AggregateApplicationBuilder;
import org.springframework.cloud.stream.app.file.source.FileSourceConfiguration;
import org.springframework.cloud.stream.app.frame.grabber.source.FrameGrabberSourceConfiguration;
import org.springframework.cloud.stream.app.image.recognition.processor.ImageRecognitionProcessorConfiguration;
import org.springframework.cloud.stream.app.image.viewer.sink.ImageViewerSinkConfiguration;
import org.springframework.cloud.stream.app.object.detection.processor.ObjectDetectionProcessorConfiguration;
import org.springframework.cloud.stream.app.pose.estimation.processor.PoseEstimationProcessorConfiguration;

@SpringBootApplication
public class SpringBootTensorflowDemoApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootTensorflowDemoApplication.class, args);
	}

	public enum DemoType {object_detection, pose_estimation, instance_segmentation, image_recognition}

	@Value("${demoType:pose_estimation}")
	private DemoType demoType = DemoType.object_detection;

	private static final String[] FRAME_GRABBER_ARGUMENTS = new String[] { "--frame.grabber.width=320", "--frame.grabber.height=240", "--frame.grabber.captureInterval=100" };

	@Override
	public void run(String... args) {

		System.out.println("Starting [" + demoType + "] Demo Application");

		switch (demoType) {
		case pose_estimation:
			// Pose Estimation:
			//  - mobilenet_thin [ 7.5MB] [320x240] -> ~2 FPS
			//  - cmu            [2000MB] [320x240] -> ~1/10 FPS
			new AggregateApplicationBuilder().web(false)
					.from(FrameGrabberSourceConfiguration.class).namespace("source").args(FRAME_GRABBER_ARGUMENTS)
					.via(PoseEstimationProcessorConfiguration.class).namespace("pose-estimation")
					.args("--tensorflow.mode=header",
							"--tensorflow.modelFetch=Openpose/concat_stage7",
							"--tensorflow.model=https://dl.bintray.com/big-data/generic/2018-30-05-mobilenet_thin_graph_opt.pb",
							//"--tensorflow.model=https://dl.bintray.com/big-data/generic/2018-05-14-cmu-graph_opt.pb",
							"--tensorflow.pose.estimation.bodyDrawingColorSchema=monochrome")
					.to(ImageViewerSinkConfiguration.class).namespace("sink").args("--image.viewer.title=Pose Estimation")
					.run(args);
			break;
		case object_detection:
			// Object Detection:
			//  - ssd_mobilenet_v1_0.75_depth              [17.5MB] [320x240] -> ~ 1/4 FPS
			//  - ssd_mobilenet_v1_ppn_shared_box_predictor  [10MB] [320x240] ->  ~ 1/4 FPS
			//  - ssdlite_mobilenet_v2_coco                  [20MB] [320x240] ->  ~ 1/4 FPS
			//  - faster_rcnn_resnet101_coco                [187MB] [320x240] ->  ~ 1/19 FPS
			new AggregateApplicationBuilder().web(false)
					.from(FrameGrabberSourceConfiguration.class).namespace("source").args(FRAME_GRABBER_ARGUMENTS)
					.via(ObjectDetectionProcessorConfiguration.class).namespace("object-detection")
					.args("--tensorflow.mode=header",
							"--tensorflow.model=https://dl.bintray.com/big-data/generic/ssd_mobilenet_v1_ppn_shared_box_predictor_300x300_coco14_sync_2018_07_03_frozen_inference_graph.pb",
							// "--tensorflow.model=https://dl.bintray.com/big-data/generic/ssd_mobilenet_v1_0.75_depth_300x300_coco14_sync_2018_07_03_frozen_inference_graph.pb",
							//"--tensorflow.model=http://dl.bintray.com/big-data/generic/ssdlite_mobilenet_v2_coco_2018_05_09_frozen_inference_graph.pb",
							//"--tensorflow.model=http://dl.bintray.com/big-data/generic/faster_rcnn_resnet101_coco_2018_01_28_frozen_inference_graph.pb",
							"--tensorflow.modelFetch=detection_scores,detection_classes,detection_boxes,num_detections",
							"--tensorflow.object.detection.labels=http://dl.bintray.com/big-data/generic/mscoco_label_map.pbtxt")
					.to(ImageViewerSinkConfiguration.class).namespace("sink").args("--image.viewer.title=Object Detection")
					.run(args);
			break;
		case instance_segmentation:
			// Instance Segmentation:
			//  - mask_rcnn_inception_v2_coco [64MB] [320x240] ->  ~ 1/12 FPS
			new AggregateApplicationBuilder().web(false)
					.from(FrameGrabberSourceConfiguration.class).namespace("source").args(FRAME_GRABBER_ARGUMENTS)
					.via(ObjectDetectionProcessorConfiguration.class).namespace("instance-segmentation")
					.args("--tensorflow.mode=header",
							"--tensorflow.model=http://dl.bintray.com/big-data/generic/mask_rcnn_inception_v2_coco_2018_01_28_frozen_inference_graph.pb",
							"--tensorflow.modelFetch=detection_scores,detection_classes,detection_boxes,detection_masks,num_detections",
							"--tensorflow.object.detection.labels=http://dl.bintray.com/big-data/generic/mscoco_label_map.pbtxt")
					.to(ImageViewerSinkConfiguration.class).namespace("sink").args("--image.viewer.title=Instance Segmentation")
					.run(args);
			break;
		case image_recognition:
			// Image Recognition
			new AggregateApplicationBuilder().web(false)
					.from(FileSourceConfiguration.class).namespace("source").args("--file.directory=" + "/tmp/input", "--file.consumer.mode=contents")
					.via(ImageRecognitionProcessorConfiguration.class).namespace("image-recognition")
					.args("--tensorflow.mode=header",
							"--tensorflow.modelFetch=output",
							"--tensorflow.model=https://dl.bintray.com/big-data/generic/tensorflow_inception_graph.pb",
							"--tensorflow.image.recognition.labels=https://dl.bintray.com/big-data/generic/imagenet_comp_graph_label_strings.txt")
					.to(ImageViewerSinkConfiguration.class).namespace("sink").args("--image.viewer.title=Image Recognition")
					.run(args);
			break;
		}

		//new AggregateApplicationBuilder().web(false)
		//		.from(FileSourceConfiguration.class).namespace("source").args("--file.directory=" + "/tmp/input", "--file.consumer.mode=contents")
		//		.via(PoseEstimationProcessorConfiguration.class).namespace("pose").args("--tensorflow.mode=header")
		//		.to(FileSinkConfiguration.class).namespace("sink").args("--file.directory=" + "/tmp/output", "--file.name-expression='headers[file_name]'")
		//		.run(args);
	}
}
