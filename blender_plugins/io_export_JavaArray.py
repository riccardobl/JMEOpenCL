# ##### BEGIN GPL LICENSE BLOCK #####
#
#  This program is free software; you can redistribute it and/or
#  modify it under the terms of the GNU General Public License
#  as published by the Free Software Foundation; either version 2
#  of the License, or (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software Foundation,
#  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
#
# ##### END GPL LICENSE BLOCK #####


bl_info = {
    "name": "Java Export",
    "author": "Riccardo B. (based on OGL Export by Krzysztof Solek)",
    "blender": (2, 5, 7),
    "api": 35622,
    "location": "File > Import-Export",
    "description": "Export mesh data into Java Arrays",
    "warning": "",
    "wiki_url": "",
    "tracker_url": "",
    "category": "Import-Export"}

import os
import bpy
from bpy.props import CollectionProperty, StringProperty, BoolProperty
from bpy_extras.io_utils import ImportHelper, ExportHelper


class ExportJava(bpy.types.Operator, ExportHelper):
    bl_idname = "java.export"
    bl_label = "Java Export"

    filename_ext = ".java"
    filter_glob = StringProperty(default="*.java", options={'HIDDEN'})

    entire_scene = BoolProperty(name="Export everything", description="Export everything", default=True)

    inline = BoolProperty(name="Inline Data", description="Inline Data", default=True)


    @classmethod
    def poll(cls, context):
        return context.active_object != None

    def execute(self, context):
        filepath = self.filepath
        filepath = bpy.path.ensure_ext(filepath, self.filename_ext)
        return export(filepath, self.entire_scene,self.inline)

    def draw(self, context):
        layout = self.layout
        row = layout.row()
        row.prop(self, "entire_scene")
        row.prop(self, "inline")


def menu_func_export(self, context):
    self.layout.operator(ExportJava.bl_idname, text="Java Arrays (.java)")


def register():
    bpy.utils.register_module(__name__)

    bpy.types.INFO_MT_file_export.append(menu_func_export)


def unregister():
    bpy.utils.unregister_module(__name__)

    bpy.types.INFO_MT_file_export.remove(menu_func_export)

if __name__ == "__main__":
    register()
    
   
    
    
    
obj_names=[]    # names of meshes in "C-suitable" format
vtx = []      # list of dictionaries for each mesh
faces = []    # list of lists
vl = []       # list of vertices for each mesh
nl = []       # list of normals for each mesh
uvl =   []    # list of UV coords for each mesh
obj_mtx=[]  # list of local transformations for each object
obj_cnt =   0   # object count
max_vcnt=   0   # qty of vertices for biggest mesh  

def buildData (obj, msh, name):
    global obj_cnt
    global obj_names     # names of meshes in "C-suitable" format
    global vtx           # list of dictionaries for each mesh
    global faces         # list of lists
    global vl            # list of vertices for each mesh
    global nl            # list of normals for each mesh
    global uvl          # list of UV coords for each mesh
    global obj_mtx      # list of local transformations for each object

    lvdic = {} # local dictionary
    lfl = [] # lcoal faces index list
    lvl = [] # local vertex list
    lnl = [] # local normal list
    luvl = [] # local uv list
    lvcnt = 0 # local vertices count
    isSmooth = False
    hasUV = True    # true by default, it will be verified below
    
    print("Building for: %s\n"%obj.name)

    if (len(msh.tessface_uv_textures)>0):
        if (msh.tessface_uv_textures.active is None):
            hasUV=False
    else:
        hasUV = False
     
    if (hasUV):
        activeUV = msh.tessface_uv_textures.active.data
        
    obj_names.append(clearName(name))
    obj_cnt+=1
    
    for i,f in enumerate(msh.tessfaces):
        isSmooth = f.use_smooth
        tmpfaces = []
        for j,v in enumerate(f.vertices):  
            vec = msh.vertices[v].co
            vec = r3d(vec)
            
            if (isSmooth):  # use vertex normal
                nor = msh.vertices[v].normal
            else:           # use face normal
                nor = f.normal
            
            nor = r3d(nor)
            
            if (hasUV):
                co = activeUV[i].uv[j]
                co = r2d(co)
            else:
                co = (0.0, 0.0)
            
            key = vec, nor, co
            vinx = lvdic.get(key)
            
            if (vinx is None): # vertex not found
                lvdic[key] = lvcnt
                lvl.append(vec)
                lnl.append(nor)
                luvl.append(co)
                tmpfaces.append(lvcnt)
                lvcnt+=1
            else:
                inx = lvdic[key]
                tmpfaces.append(inx)
        
        if (len(tmpfaces)==3): 
            lfl.append(tmpfaces)
        else:
            lfl.append([tmpfaces[0], tmpfaces[1], tmpfaces[2]])
            lfl.append([tmpfaces[0], tmpfaces[2], tmpfaces[3]])

    
    #update global lists and dictionaries
    vtx.append(lvdic)        
    faces.append(lfl)
    vl.append(lvl)
    nl.append(lnl)
    uvl.append(luvl)
    obj_mtx.append(obj.matrix_local)

    
def r3d(v):
    return round(v[0],6), round(v[1],6), round(v[2],6)
    

def r2d(v):
    return round(v[0],6), round(v[1],6)
    

def clearName(name):
    tmp=name.upper()
    ret=""
    for i in tmp:
        if (i in " ./\-+#$%^!@"):
            ret=ret+"_"
        else:
            ret=ret+i
    return ret    
    
def writeData(filename,file,inline):
      
    if inline:   
        file.write ("""\n
            ArrayList<Vector3f> verticesList = new ArrayList<Vector3f>();\n
            ArrayList<Vector3f> normalsList = new ArrayList<Vector3f>();\n
            ArrayList<Vector2f> textureCoordinateList = new ArrayList<Vector2f>();\n
            ArrayList<Integer> indicesList = new ArrayList<Integer>();\n
            \n""")
        
     
        
        #write verictes table
        for i, d in enumerate(vl):
            file.write ("\t/* %s: %d vertices */\n"%(obj_names[i],len(d)))
            for j in range(0,len(d)):
                file.write ("\tverticesList.add(new Vector3f("+str(vl[i][j][0])+"f,"+str(vl[i][j][1])+"f,"+str(vl[i][j][2])+"f));\n")     
                file.write ("\tnormalsList.add(new Vector3f("+str(nl[i][j][0])+"f,"+str(nl[i][j][1])+"f,"+str(nl[i][j][2])+"f));\n")     
                file.write ("\ttextureCoordinateList.add(new Vector2f("+str(uvl[i][j][0])+"f,"+str(uvl[i][j][1])+"f));\n")     
    
        
        for i in range(len(faces)):
            for j, f in enumerate(faces[i]):
                for i in f:
                    file.write ("\tindicesList.add("+str(i)+");\n")     
    
    else:
          
        file.write ("""\n
        ArrayList<Vector3f> verticesList = new ArrayList<Vector3f>();
        ArrayList<Vector3f> normalsList = new ArrayList<Vector3f>();
        ArrayList<Vector2f> textureCoordinateList = new ArrayList<Vector2f>();
        ArrayList<Integer> indicesList = new ArrayList<Integer>();
        try{
            FileInputStream r = new FileInputStream(\""""+filename+""".dat");
            Scanner s = new Scanner(r);
            byte ci=0;
            while (s.hasNextLine()){
                String line=s.nextLine();
                String tokens[]=line.split("#");
                for(String token:tokens){
                    if(ci==0||ci==1){
                            String vcs[]=token.split(";");
                            Vector3f v3f=new Vector3f(Float.parseFloat(vcs[0]),Float.parseFloat(vcs[1]),Float.parseFloat(vcs[2]));                            
                            if(ci==0)verticesList.add(v3f);
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
        }catch(Exception e){e.printStackTrace();}   
            \n""")
        
        vo=""
        no=""
        to=""
        io=""
        
        #write verictes table
        for i, d in enumerate(vl):
            file.write ("\t/* %s: %d vertices */\n"%(obj_names[i],len(d)))
            for j in range(0,len(d)):
                vo+=("#" if vo!="" else "")+str(vl[i][j][0])+";"+str(vl[i][j][1])+";"+str(vl[i][j][2])
                no+=("#" if no!="" else "")+str(nl[i][j][0])+";"+str(nl[i][j][1])+";"+str(nl[i][j][2])     
                to+=("#" if to!="" else "")+str(uvl[i][j][0])+";"+str(uvl[i][j][1])    
    
        
        for i in range(len(faces)):
            for j, f in enumerate(faces[i]):
                for i in f:
                    io+=("#" if io!="" else "")+str(i)
                    
        file = open(filename+".dat", "w", newline="\n")
        file.write(vo+"\n"+no+"\n"+to+"\n"+io)
        file.close()
        
            
def save(filename,inline=True):
    file = open(filename, "w", newline="\n")
    file.write("// Export Start\n")
    writeData(filename,file,inline)
    file.write("// Export End\n")
    file.close()


def export(filename="untitled.java", entire_scene=True,inline=True):
    global obj_cnt
    global obj_names     # names of meshes in "C-suitable" format
    global vtx           # list of dictionaries for each mesh
    global faces         # list of lists
    global vl            # list of vertices for each mesh
    global nl            # list of normals for each mesh
    global uvl          # list of UV coords for each mesh
    global obj_mtx      # list of local transformations for each object

    print("--------------------------------------------------\n")
    print("Starting script:\n")
    print(filename)

    # clear all gloabl variables
    obj_names=[]    # names of meshes in "C-suitable" format
    vtx = []      # list of dictionaries for each mesh
    faces = []    # list of lists
    vl = []       # list of vertices for each mesh
    nl = []       # list of normals for each mesh
    uvl =   []    # list of UV coords for each mesh
    obj_mtx=[]  # list of local transformations for each object
    obj_cnt =   0   # object count
    max_vcnt=   0   # qty of vertices for biggest mesh


    sc = bpy.context.scene  # export MESHes from active scene

    if (entire_scene):
        for o in sc.objects:
            if (o.type=="MESH"):    # export ONLY meshes
                msh = o.to_mesh(sc,True,"PREVIEW") # prepare MESH
                buildData(o, msh, o.name)
                bpy.data.meshes.remove(msh)
    else:
        o = sc.objects.active
        msh = o.to_mesh(sc,True,'PREVIEW')
        buildData(o, msh, o.name)
        bpy.data.meshes.remove(msh)

    save(filename,inline)    
    print("Done\n")
    return {'FINISHED'}



