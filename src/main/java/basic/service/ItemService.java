package basic.service;
import basic.cachedto.LpSalesDto;
import basic.dto.ItemRequest;
import basic.dto.ItemResponse;
import basic.dto.UserSession;
import basic.entity.Item;
import basic.entity.Member;
import basic.repository.ItemRepository;
import basic.repository.MemberRepository;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import basic.repository.LpRedisRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ItemService {

    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    private final S3Service s3Service;
	private final LpRedisRepository lpRedisRepository;

    @Value("${cloud.aws.s3.bucket-url}")
    private String bucketUrl;

    public ItemService(MemberRepository memberRepository, ItemRepository itemRepository, S3Service s3Service, LpRedisRepository lpRedisRepository) {
        this.memberRepository = memberRepository;
        this.itemRepository = itemRepository;
        this.s3Service = s3Service;
		this.lpRedisRepository = lpRedisRepository;
    }

    @Transactional
    public void saveItem(ItemRequest itemRequest, MultipartFile file, HttpSession session) {
        String uploadedFileName = null;

//        if (file != null && !file.isEmpty()) {
//            try {
//                uploadedFileName = s3Service.uploadS3File(file);
//            } catch (IOException e) {
//                throw new RuntimeException("이미지 업로드 실패", e);
//            }
//        }

        Long memberId = Long.valueOf(getUserSession(session).getUserId());
        Member findMember = memberRepository.findById(memberId).orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));

        log.info("uploadFileName = {}", uploadedFileName);
        Item item = null;
        if (file == null && file.isEmpty()) {
            item = Item.tempOf(
                    findMember,
                    itemRequest.getName(),
                    itemRequest.getPrice(),
                    itemRequest.getStockQuantity());
        }else {
            item = Item.of(
                    findMember,
                    itemRequest.getName(),
                    itemRequest.getPrice(),
                    itemRequest.getStockQuantity(),
                    uploadedFileName // 업로드된 파일명을 저장
                    );
        }

        itemRepository.save(item);
    }

    public List<ItemResponse> findItems() {
        List<Item> findItems = itemRepository.findAll();

        return findItems.stream()
                .map(item -> {
                    String fullImageUrl = null;
                    if (item.getImageFileName() != null) {
                        fullImageUrl = bucketUrl + item.getImageFileName();
                    }
                    return new ItemResponse(
                            item.getId(),
                            item.getName(),
                            item.getPrice(),
                            item.getStockQuantity(),
                            item.getImageFileName(),
                            fullImageUrl,
                            item.getMember().getUsername()
                    );
                })
                .collect(Collectors.toList());
    }

    public ItemResponse findOne(Long itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new IllegalStateException("존재하지 않는 상품 입니다."));
        return ItemResponse.fromEntity(item, bucketUrl);
    }


    	// 상위 5개 LP 조회 (판매량 순)
	public List<LpSalesDto> getTopSales(int topN) {
		Set<TypedTuple<String>> zset = lpRedisRepository.getTopSales(topN);

		List<Long> itemIds = zset.stream().map(tuple -> Long.valueOf(tuple.getValue())).toList();

		List<Item> items = itemRepository.findAllById(itemIds);
		Map<Long, String> idToNameMap = items.stream().collect(Collectors.toMap(Item::getId, Item::getName));

		return zset.stream().map(tuple -> {
			Long itemId = Long.valueOf(tuple.getValue());
			String name = idToNameMap.getOrDefault(itemId, "Unknown");
			int salesCount = tuple.getScore().intValue();
			return new LpSalesDto(itemId, name, salesCount);
		}).toList();

	}

    private static UserSession getUserSession(HttpSession session) {
        UserSession userSession = (UserSession) session.getAttribute("userSession");
        if (userSession == null) {
            throw new IllegalStateException("로그인 정보가 없습니다.");
        }
        return userSession;
    }

}