package cl.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;


public class DatLoader implements AssetLoader{
    
    public static class DatOutput{
        public ArrayList<Vector3f> V,N;
        public ArrayList<Vector2f> T;
        public ArrayList<Integer> I;
    }
    
    public Object load(AssetInfo assetInfo) throws IOException {
        ArrayList<Vector3f> verticesList=new ArrayList<Vector3f>();
        ArrayList<Vector3f> normalsList=new ArrayList<Vector3f>();
        ArrayList<Vector2f> textureCoordinateList=new ArrayList<Vector2f>();
        ArrayList<Integer> indicesList=new ArrayList<Integer>();
        try{
            InputStream r=assetInfo.openStream();
            Scanner s=new Scanner(r);
            byte ci=0;
            while(s.hasNextLine()){
                String line=s.nextLine();
                String tokens[]=line.split("#");
                for(String token:tokens){
                    if(ci==0||ci==1){
                        String vcs[]=token.split(";");
                        Vector3f v3f=new Vector3f(Float.parseFloat(vcs[0]),Float.parseFloat(vcs[1]),Float.parseFloat(vcs[2]));
                        if(ci==0) verticesList.add(v3f);
                        else normalsList.add(v3f);
                    }else if(ci==2){
                        String vcs[]=token.split(";");
                        Vector2f v2f=new Vector2f(Float.parseFloat(vcs[0]),Float.parseFloat(vcs[1]));
                        textureCoordinateList.add(v2f);
                    }else if(ci==3){
                        Integer intg=Integer.parseInt(token);
                        indicesList.add(intg);
                    }
                }
                ci++;
            }
            s.close();
            r.close();
        }catch(Exception e){
            e.printStackTrace();
        }

        DatOutput out=new DatOutput();
        out.V=verticesList;
        out.N=normalsList;
        out.T=textureCoordinateList;
        out.I=indicesList;
        return out;
    }

}
