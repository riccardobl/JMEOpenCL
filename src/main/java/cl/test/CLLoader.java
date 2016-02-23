package cl.test;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;

public class CLLoader implements AssetLoader{
    public Object load(AssetInfo assetInfo) throws IOException {
        InputStream s=assetInfo.openStream();
        Scanner sc=new Scanner(s);
        String out="";
        while(sc.hasNextLine())
            out+=(out.isEmpty()?sc.nextLine():"\n"+sc.nextLine());
        sc.close();
        return out;
    }

}
