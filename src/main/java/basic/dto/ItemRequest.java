package basic.dto;

import lombok.Data;

@Data
public class ItemRequest {
    private String name;
    private int price;
    private int stockQuantity;
    private String imageFileName;

    public ItemRequest() {
    }

    public ItemRequest(String name, int price, int stockQuantity, String imageFileName) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imageFileName = imageFileName;
    }
}