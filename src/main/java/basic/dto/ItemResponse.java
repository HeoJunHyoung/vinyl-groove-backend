package basic.dto;

import basic.entity.Item;
import lombok.Data;

@Data
public class ItemResponse {
    private Long id;
    private String name;
    private int price;
    private int stockQuantity;
    private String imageFileName;
    private String imageUrl;
    private String sellerName;

    public ItemResponse() {
    }

    public ItemResponse(Long id, String name, int price, int stockQuantity, String imageFileName, String imageUrl, String sellerName) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imageFileName = imageFileName;
        this.imageUrl = imageUrl;
        this.sellerName = sellerName;
    }

    public static ItemResponse fromEntity(Item item, String bucketUrl) {
        String fullImageUrl = null;
        if (item.getImageFileName() != null) {
            fullImageUrl = bucketUrl + item.getImageFileName();
        }
        return new ItemResponse(item.getId(), item.getName(), item.getPrice(), item.getStockQuantity(),
                                item.getImageFileName(), fullImageUrl, item.getMember().getUsername());
    }
}
