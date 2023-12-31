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
    public Path getVideoExtension(MultipartFile video, Path folder) throws IOException {
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

        switch (extension) {
            case "mp4":
                path = Path.of(folder + "/" + UUID.randomUUID() + ".mp4");
                outputVideoPath = String.valueOf(path);
                Runnable parallelTask = () -> {
                };
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        compressMp4(probeResult, executor, video, folder, outputVideoPath, Path.of(inputVideo));
                    }
                });
                thread.start();
                return path;
            case "avi":
                //return compressAVI(video, folder);
            case "webm":
//                return compressWebm(video, folder);
            default:
                return null;
        }
    }

    @Async("AsyncTaskExecutor")
    Path compressMp4(MultipartFile video, Path folder) {
        try {
            //set ffmpeg and ffprobe path
            FFmpeg ffmpeg = new FFmpeg(String.valueOf(FFMPEG_PATH));
            FFprobe ffprobe = new FFprobe(String.valueOf(FFPROBE_PATH));


            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            String inputVideo = UUID.randomUUID() + "-" + video.getOriginalFilename();
            Path path = Path.of(folder + "/" + UUID.randomUUID() + ".mp4");
            String outputVideoPath = String.valueOf(path);

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

            FFmpegProbeResult in = ffprobe.probe(inputVideo);

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(in)
                    .addOutput(outputVideoPath.toString())
                    .setAudioCodec("aac")
                    .setVideoCodec("libx265")
                    .setFormat("mp4")
                    .setVideoFrameRate(FFmpeg.FPS_30)
                    .setAudioChannels(FFmpeg.AUDIO_STEREO)
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                    .addExtraArgs("-tile-columns", "6")
                    .done();


            FFmpegJob job = executor.createJob(builder);
            job.run();

            /*job = executor.createJob(builder, new ProgressListener() {
                final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);

                @Override
                public void progress(Progress progress) {
                    if (!"N/A".equals(progress.out_time_ns)) {
                        double percentage = (progress.out_time_ns / duration_ns) * 100;
                        System.out.println("Progress: " + (int) percentage + "%");
                    } else {
                        throw new AppException("Error while compressing video", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            });

            job.run();
            if (job.getState() == FFmpegJob.State.FAILED) {
                throw new AppException("Error while compressing video", HttpStatus.INTERNAL_SERVER_ERROR);
            }*/

            Files.delete(videoInputPath);
            //check if file deleted
            if (Files.exists(videoInputPath)) {
                Files.delete(videoInputPath);
            }

            return Path.of(outputVideoPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }


    @Async("AsyncTaskExecutor")
    void compressAvi(FFmpegProbeResult probeResult, FFmpegExecutor executor, MultipartFile video, Path folder, String outputVideoPath, Path videoInputPath) {
        try {

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

            FFmpegJob job = executor.createJob(builder);
            job.run();

            Files.delete(videoInputPath);
            //check if file deleted
            if (Files.exists(videoInputPath)) {
                Files.delete(videoInputPath);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Error: " + e.getStackTrace());
        }
    }

    @Async("AsyncTaskExecutor")
    void compressMp4(FFmpegProbeResult probeResult, FFmpegExecutor executor, MultipartFile video, Path folder, String outputVideoPath, Path videoInputPath) {
        try {

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

            FFmpegJob job = executor.createJob(builder);
            job.run();

            Files.delete(videoInputPath);
            //check if file deleted
            if (Files.exists(videoInputPath)) {
                Files.delete(videoInputPath);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Error: " + e.getStackTrace());
        }
    }


    @Async("AsyncTaskExecutor")
    Path compressAVI(MultipartFile video, Path folder) {
        try {
            FFmpeg ffmpeg = new FFmpeg(Objects.requireNonNull(getClass().getResource("/ffmpeg/bin/ffmpeg.exe")).getPath());
            FFprobe ffprobe = new FFprobe(Objects.requireNonNull(getClass().getResource("/ffmpeg/bin/ffprobe.exe")).getPath());

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            String videoInputPath = UUID.randomUUID() + "-" + video.getOriginalFilename();
            Path path = Path.of(folder + "/" + UUID.randomUUID() + ".avi");
            String outputVideoPath = String.valueOf(path);

            //save video to file by path
            Files.write(Path.of(videoInputPath), video.getBytes());

            FFmpegProbeResult in = ffprobe.probe(videoInputPath);

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(in)
                    .addOutput(outputVideoPath)
                    .setAudioCodec("libmp3lame")
                    .setVideoCodec("mpeg4")
                    .setFormat("avi")
                    .setVideoFrameRate(FFmpeg.FPS_30)
                    .setAudioChannels(FFmpeg.AUDIO_STEREO)
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                    .done();

           /* Files.delete(Path.of(videoInputPath));
            if (Files.exists(videoInputPath)) {
                Files.delete(videoInputPath);
            }*/

            return Path.of(outputVideoPath);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Error: " + e.getStackTrace());
            return null;
        }
    }


    @Async("AsyncTaskExecutor")
    Path compressWebm(MultipartFile video, Path folder) {
        FFmpegJob job = null;
        try {
            FFmpeg ffmpeg = new FFmpeg(String.valueOf(FFMPEG_PATH));
            FFprobe ffprobe = new FFprobe(String.valueOf(FFPROBE_PATH));

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            String inputVideo = UUID.randomUUID() + "-" + video.getOriginalFilename();
            Path path = Path.of(folder + "/" + UUID.randomUUID() + ".webm");
            String outputVideoPath = String.valueOf(path);

            Path folderDirectory = Path.of(currentDirectory + "/" + folder);
            if (!Files.exists(folderDirectory)) {
                try {
                    Files.createDirectory(folderDirectory);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }

            Path videoInputPath = Path.of(inputVideo);
            Files.write(videoInputPath, video.getBytes());

            FFmpegProbeResult in = ffprobe.probe(inputVideo);

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(in)
                    .addOutput(outputVideoPath)
                    .setAudioCodec("libopus")
                    .setVideoCodec("libvpx-vp9")
                    .setFormat("webm")
                    .setVideoFrameRate(FFmpeg.FPS_30)
                    .setAudioChannels(FFmpeg.AUDIO_STEREO)
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                    .addExtraArgs("-tile-columns", "6")
                    .done();

            job = executor.createJob(builder);
            job.run();

            Files.delete(videoInputPath);
            //check if file deleted
            if (Files.exists(videoInputPath)) {
                Files.delete(videoInputPath);
            }

            return Path.of(outputVideoPath);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
