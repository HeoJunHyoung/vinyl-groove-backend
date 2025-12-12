package basic.cachedto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LpSalesDto {
	private Long id;
	private String name;
	private int salesCount;
}