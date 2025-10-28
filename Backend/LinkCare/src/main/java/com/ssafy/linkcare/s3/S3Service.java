package com.ssafy.linkcare.s3;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    /**
     * S3에 파일 업로드 (profile/ 폴더 아래에 저장)
     * @param file 업로드할 파일
     * @return 업로드된 파일의 URL
     */
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String filename = "profile/" + UUID.randomUUID() + "_" + originalFilename;

        try {
            // S3에 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(filename)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

            s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 업로드된 파일의 URL 반환
            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucket, region, filename);

            log.info("S3 파일 업로드 성공: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("S3 파일 업로드 실패", e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * S3에서 파일 삭제
     * @param fileUrl 삭제할 파일의 URL
     */
    public void deleteFile(String fileUrl) {
        try {
            // URL에서 파일명 추출 (profile/UUID_filename.jpg)
            int index = fileUrl.indexOf(".com/");
            if (index == -1) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 S3 URL입니다");
            }
            String key = fileUrl.substring(index + 5);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 성공: {}", key);

        } catch (Exception e) {
            log.error("S3 파일 삭제 실패", e);
            throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
        }
    }
}