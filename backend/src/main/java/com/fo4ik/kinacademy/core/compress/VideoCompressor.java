package com.fo4ik.kinacademy.core.compress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.io.FilenameUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;


@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Builder
@Service
@NoArgsConstructor
@AllArgsConstructor
public class VideoCompressor {
    //TODO Optimize this class

    @Builder.Default
    private final Path FFMPEG_PATH = Path.of("backend/ffmpeg/bin/ffmpeg.exe");
    @Builder.Default
    private final Path FFPROBE_PATH = Path.of("backend/ffmpeg/bin/ffprobe.exe");
    @Builder.Default
    private final String currentDirectory = System.getProperty("user.dir");


    @Async("AsyncTaskExecutor")
    public String getVideoExtension(MultipartFile video, Path folder) throws IOException {
        String extension = FilenameUtils.getExtension(video.getOriginalFilename());

        FFmpeg ffmpeg = new FFmpeg(String.valueOf(FFMPEG_PATH));
        FFprobe ffprobe = new FFprobe(String.valueOf(FFPROBE_PATH));

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        String inputVideo = UUID.randomUUID() + "-" + video.getOriginalFilename();

        Path folderDirectory = Path.of(currentDirectory + "/" + folder);
        if (!Files.exists(folderDirectory)) {
            try {
                System.out.println("Creating directory: " + folderDirectory);
                Files.createDirectory(folderDirectory);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        Path videoInputPath = Path.of(inputVideo);
        Files.write(videoInputPath, video.getBytes());

        FFmpegProbeResult probeResult = ffprobe.probe(inputVideo);
        Path path;
        String outputVideoPath;
        String videoName;

        switch (extension) {
            case "mp4":
                videoName = UUID.randomUUID() + ".mp4";
                path = Path.of(folder + "/" + videoName);
                outputVideoPath = String.valueOf(path);
                Thread mp4Thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        compressMp4(probeResult, executor, video, folder, outputVideoPath, Path.of(inputVideo));
                    }
                });
                mp4Thread.start();
                return videoName;
            case "avi":
                videoName = UUID.randomUUID() + ".avi";
                path = Path.of(folder + "/" + videoName);
                outputVideoPath = String.valueOf(path);
                Thread aviThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        compressAvi(probeResult, executor, video, folder, outputVideoPath, Path.of(inputVideo));
                    }
                });
                aviThread.start();
                return videoName;
            case "webm":
//                return compressWebm(video, folder);
            default:
                return null;
        }
    }


    /**
     * Asynchronously compresses an AVI video file using FFmpeg.
     *
     * @param probeResult     The FFmpegProbeResult for the input video
     * @param executor        The FFmpegExecutor for executing FFmpeg commands
     * @param video           The input video file
     * @param folder          The folder where the compressed video will be stored
     * @param outputVideoPath The path for the compressed video file
     * @param videoInputPath  The path for the input video file
     */
    @Async("AsyncTaskExecutor")
    void compressAvi(FFmpegProbeResult probeResult, FFmpegExecutor executor,
                     MultipartFile video, Path folder, String outputVideoPath,
                     Path videoInputPath) {
        try {
            // Configure FFmpegBuilder for video compression
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(probeResult)
                    .addOutput(outputVideoPath)
                    .setAudioCodec("libmp3lame")
                    .setVideoCodec("mpeg4")
                    .setFormat("avi")
                    .setVideoFrameRate(FFmpeg.FPS_30)
                    .setAudioChannels(FFmpeg.AUDIO_STEREO)
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                    .done();

            // Create and run FFmpegJob
            FFmpegJob job = executor.createJob(builder);
            job.run();

            // Delete the original video file after compression
            Files.delete(videoInputPath);

            // Check if the file deletion was successful and delete again if needed
            if (Files.exists(videoInputPath)) {
                Files.delete(videoInputPath);
            }
        } catch (Exception e) {
            // Handle any exceptions that may occur during the compression process
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Asynchronously compresses an MP4 video file using FFmpeg.
     *
     * @param probeResult     The FFmpegProbeResult for the input video
     * @param executor        The FFmpegExecutor for executing FFmpeg commands
     * @param video           The input video file
     * @param folder          The folder where the compressed video will be stored
     * @param outputVideoPath The path for the compressed video file
     * @param videoInputPath  The path for the input video file
     */
    @Async("AsyncTaskExecutor")
    void compressMp4(FFmpegProbeResult probeResult, FFmpegExecutor executor,
                     MultipartFile video, Path folder, String outputVideoPath,
                     Path videoInputPath) {
        try {
            // Configure FFmpegBuilder for video compression
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(probeResult)
                    .addOutput(outputVideoPath.toString())
                    .setAudioCodec("aac")
                    .setVideoCodec("libx265")
                    .setFormat("mp4")
                    .setVideoFrameRate(FFmpeg.FPS_30)
                    .setAudioChannels(FFmpeg.AUDIO_STEREO)
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                    .addExtraArgs("-tile-columns", "6")
                    .done();

            // Create and run FFmpegJob
            FFmpegJob job = executor.createJob(builder);
            job.run();

            // Delete the original video file after compression
            Files.delete(videoInputPath);

            // Check if the file deletion was successful and delete again if needed
            if (Files.exists(videoInputPath)) {
                Files.delete(videoInputPath);
            }
        } catch (Exception e) {
            // Handle any exceptions that may occur during the compression process
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
