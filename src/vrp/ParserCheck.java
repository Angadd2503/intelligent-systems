package vrp;

import java.io.*;
import java.util.*;

public class ParserCheck {
    public static void main(String[] args) {
        List<ItemsParser.Item> items = ItemsParser.readItems("data/items.txt");

        for (ItemsParser.Item item : items) {
            System.out.println(item.name + " at (" + item.x + ", " + item.y + ")");
        }
    }
}
