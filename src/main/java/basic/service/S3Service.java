package basic.service;

import basic.entity.AttachmentFile;
import basic.repository.AttachmentFileRepository;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
public class S3Service {

    private final AmazonS3 amazonS3;
    private final AttachmentFileRepository fileRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final String DIR_NAME = "items/images";

    public S3Service(AmazonS3 amazonS3, AttachmentFileRepository fileRepository) {
        this.amazonS3 = amazonS3;
        this.fileRepository = fileRepository;
    }

    // [1] 파일 업로드
    @Transactional
    public String uploadS3File(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("파일이 비어 있거나 null입니다.");
        }

        AttachmentFile attachmentFile = createAttachmentFile(file);
        Long fileNo = saveAttachmentFileToDB(attachmentFile);
        saveWithS3(file, fileNo, attachmentFile, attachmentFile.getAttachmentFileName());
        log.info("attachmentFileName = {}", attachmentFile.getAttachmentFileName());
        return attachmentFile.getAttachmentFileName(); // ✅ 반환
    }

    // [2] 파일 다운로드
    @Transactional
    public ResponseEntity<Resource> downloadS3File(long fileNo) {

        // 1. DB에서 파일 메타데이터 조회
        AttachmentFile attachmentFile = fileRepository.findById(fileNo)
                .orElseThrow(() -> new NoSuchElementException("파일이 존재하지 않습니다: " + fileNo));

        // 2. S3 객체 키 구성 (업로드 시와 동일한 규칙 적용)
        String s3Key = DIR_NAME + "/" + attachmentFile.getAttachmentFileName();

        // 3. S3에서 객체 가져오기
        S3Object object = amazonS3.getObject(bucketName, s3Key);
        S3ObjectInputStream inputStream = object.getObjectContent();

        // 4. Resource로 변환
        Resource resource = new InputStreamResource(inputStream);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition
                .builder("attachment")
                .filename(attachmentFile.getAttachmentOriginalFileName())
                .build());

        return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
    }

    private AttachmentFile createAttachmentFile(MultipartFile file) {
        UUID uuid = UUID.randomUUID();
//        String savePath = "C://Cloud Engineering//98.data//" + DIR_NAME;
        String savePath = "/home/ubuntu/" + DIR_NAME;
        String attachmentOriginalFileName = file.getOriginalFilename();
        String attachmentFileName = uuid.toString() + "_" + attachmentOriginalFileName;
        Long attachmentFileSize = file.getSize();

        return AttachmentFile.builder()
                .attachmentFileName(attachmentFileName)
                .attachmentOriginalFileName(attachmentOriginalFileName)
                .filePath(savePath)
                .attachmentFileSize(attachmentFileSize)
                .build();
    }

    private Long saveAttachmentFileToDB(AttachmentFile attachmentFile) {
        return fileRepository.save(attachmentFile).getAttachmentFileNo();
    }

    private void saveWithS3(MultipartFile file, Long fileNo, AttachmentFile attachmentFile, String attachmentFileName) throws IOException {
        // S3 물리적으로 저장
        if (fileNo != null) {
            // 임시 파일 저장
            File uploadFile = new File(attachmentFile.getFilePath() + "//" + attachmentFileName);
            file.transferTo(uploadFile);

            // S3 파일 전송
            // bucket : 버킷
            // key : 객체의 저장경로 + 객체의 이름
            // file : 물리적인 리소스
            String key = DIR_NAME + "/" + uploadFile.getName();
            amazonS3.putObject(bucketName, key, uploadFile);

            // S3 파일 전송 후에는 저장된 임시 파일 제거 (과부하 방지)
            if (uploadFile.exists()) {
                uploadFile.delete();
            }

        }
    }
}

