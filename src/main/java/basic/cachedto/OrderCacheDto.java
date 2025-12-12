package basic.cachedto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCacheDto {
	private Long orderId;
	private Long memberId;
	private Long itemId;
	private int count;
	private int totalPrice;
}