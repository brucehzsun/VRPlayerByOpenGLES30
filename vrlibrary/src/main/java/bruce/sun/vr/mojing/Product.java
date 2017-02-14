package bruce.sun.vr.mojing;

import java.util.ArrayList;
import java.util.List;

public class Product {
	public String mURL;
	public String mDisplay;
	public String mKey;
	public List<Glass> mGlassList;

	public Product() {
		mGlassList = new ArrayList<Glass>();
	}

}
