package bruce.sun.vr.mojing;

import java.util.ArrayList;
import java.util.List;

public class Manufacturer {
	public String mURL;
	public String mDisplay;
	public String mKEY;
	public List<Product> mProductList;

	public Manufacturer() {
		mProductList = new ArrayList<Product>();
	}

	public List<String> getAllDisplay() {
		List<String> displays = new ArrayList<String>();

		List<String> glassDisplay = new ArrayList<String>();

		List<String> DisplayArray = new ArrayList<String>();

		for (Product p : mProductList) {
			for (Glass g : p.mGlassList) {
				if (g.mDisplay == null) {
					glassDisplay.add(" ");
				} else {
					glassDisplay.add(g.mDisplay);
				}
				displays.add(p.mDisplay);
			}

		}
		for (int i = 0; i < displays.size(); i++) {
			String pDisplay = displays.get(i);
			String gDisplay = glassDisplay.get(i);
			String DisPlay = pDisplay + " " + gDisplay;
			DisplayArray.add(DisPlay);
		}

		return DisplayArray;
	}

}
