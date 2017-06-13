package misc;

import javax.swing.BorderFactory;

import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public class Utils
{
	private static Border stdBorder;

	public static TitledBorder createTitledBorder(String title)
	{
		if(stdBorder == null)
			stdBorder = BorderFactory.createEtchedBorder();

		return new TitledBorder(stdBorder, title);
	}
}