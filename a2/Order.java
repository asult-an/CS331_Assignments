/* Helper class for an order in the OnlineOrder app
   @author David Furcy
   @version CS 391 - Spring 2024 - A2
*/

import java.util.*;

class Order
{
    // each item (String) may appear one or more times in an order
    private TreeMap<String,Integer> items;

    Order()
    {
        items = new TreeMap<String,Integer>();

    }// constructor

    /* increment by 1 the count for the given item in the order, or insert the
       item into the order with count 1, if the item was not yet part of the
       order
     */
    void addItem(String item)
    {
        if (items.containsKey(item))
        {
            items.put(item, 1 + items.get(item));
        } else
        {
            items.put(item, 1);
        }
    }// addItem method

    /* convert the order to its string representation
     */
    public String toString ()
    {
        String result = "Your order is";
        if (items.size() > 0)
        {
            result += ":\n";
            for(Map.Entry<String,Integer> entry : items.entrySet()) {
                String item = entry.getKey();
                int count = entry.getValue();
                result += "  " + count + "x " + item + "\n";
            }
        } else
        {
            result += " empty.";
        }
        return result;
    }// toString method
    
}// Order class
