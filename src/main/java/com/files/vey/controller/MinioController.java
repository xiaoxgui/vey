package com.files.vey.controller;

import com.files.vey.object.ObjectInfo;
import io.minio.*;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.minio.ListObjectsArgs;
import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class MinioController {

    private final MinioClient minioClient;

    private String bucketName = "vey";

    public MinioController(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    //检测桶是否存在并创建
    @PostConstruct
    public void init() throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    @GetMapping("/listBuckets")
    public ResponseEntity<?> listBuckets() {
        try {
            List<String> buckets = new ArrayList<>();
            for (Bucket bucket : minioClient.listBuckets()) {
                buckets.add(bucket.name());
            }
            return ResponseEntity.ok().body(Map.of("buckets", buckets));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "获取桶列表失败"));
        }
    }

    @PostMapping("/createBucket")
    public ResponseEntity<?> createBucket(@RequestParam String bucketName) {
        try {
            this.bucketName = bucketName;
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            return ResponseEntity.ok().body(Map.of("message", "操作成功"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "操作失败"));
        }
    }

    @PostMapping("/deleteBucket")
    public ResponseEntity<?> deleteBucket(@RequestParam String bucketName) {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (bucketExists) {
                Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build());
                if (results.iterator().hasNext()) {
                    return ResponseEntity.ok().body(Map.of("message", "Не удалось удалить. Сначала очистите файл."));
                } else {
                    minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
                    return ResponseEntity.ok().body(Map.of("message", "删除成功"));
                }
            } else {
                return ResponseEntity.ok().body(Map.of("message", "桶不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "删除失败"));
        }
    }

    @PostMapping("/upload")
    public Map<String, String> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam String bucketName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(file.getOriginalFilename())
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build());
            return Map.of("message", "Файл успешно загружен.");
        } catch (Exception e) {
            return Map.of("message", "文件上传失败: " + e.getMessage());//map.of返回json数据，组织页面报错
        }
    }


    @DeleteMapping("/delete/{objectName}")
    public String deleteFile(@PathVariable String objectName,@RequestParam String bucketName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            return "Файл успешно удален";
        } catch (Exception e) {
            return "文件删除失败: " + e.getMessage();
        }
    }

    @GetMapping("/download/{objectName}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String objectName,@RequestParam String bucketName) {
        try {
            InputStreamResource resource = new InputStreamResource(
                    minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectName)
                                    .build()));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new InputStreamResource(new ByteArrayInputStream(("File download failed: " + e.getMessage()).getBytes())));
        }
    }

    @GetMapping("/list")
    public List<ObjectInfo> listObjects(@RequestParam String bucketName) {
        List<ObjectInfo> objectList = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                ObjectInfo objectInfo = new ObjectInfo(item.objectName(), item.lastModified(), item.size());
                objectList.add(objectInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return objectList;

    }
}