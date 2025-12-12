package basic.entity;

import basic.exception.NotEnoughStockException;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Item extends BaseEntity{

    @Id @GeneratedValue
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String name;

    private int price;

    private int stockQuantity;

    private String imageFileName;

    protected Item() {}

    private Item(Member member, String name, int price, int stockQuantity, String imageFileName){
        this.member = member;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imageFileName = imageFileName;
    }

    private Item(Member member, String name, int price, int stockQuantity){
        this.member = member;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public static Item of(Member member, String name, int price, int stockQuantity, String imageFileName) {
        return new Item(member, name, price, stockQuantity, imageFileName);
    }

    public static Item tempOf(Member member, String name, int price, int stockQuantity) {
        return new Item(member, name, price, stockQuantity);
    }
    public void addStock(int quantity) {
        this.stockQuantity += quantity;
    }

    public void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new NotEnoughStockException("need more stock");
        }
        this.stockQuantity = restStock;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }


}