package org.scott;

import java.util.HashMap;
import java.util.Map;

//Silly method so I can pass items to the writer service easily
//keeping them all organized so I don't accidentally create write conflicts
public class AddressMenu {
    String address;
    String state;
    String city;
    Map<String, Integer> menu = new HashMap<String, Integer>();

    public AddressMenu(String address, String state, String city, Map<String, Integer> menu) {
        this.address = address;
        this.state = state;
        this.city = city;
        this.menu = menu;
    }

    public String getAddress() {
        return address;
    }

    public String getState() {
        return state;
    }

    public String getCity() {
        return city;
    }

    public Map<String, Integer> getMenu() {
        return menu;
    }
}