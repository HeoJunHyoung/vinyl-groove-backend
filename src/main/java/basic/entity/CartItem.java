package basic.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class CartItem {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    private Item item;

    private int count;

    protected CartItem() {}

    public CartItem(Member member, Item item, int count) {
        this.member = member;
        this.item = item;
        this.count = count;
    }

    public static CartItem of(Member member, Item item, int count) {
        return new CartItem(member, item, count);
    }

    public void addCount(int count) {
        this.count += count;
    }
}