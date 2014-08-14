__kernel void main(
        __global float *vertices,
        __global float *particles_position,
        __global float *shape_vertices,
        __constant float *gravity_point,
        __global float *particles_velocity,
        int shape_vertices_count,
        int vertices_count,
        float tpf
        ){
        
    unsigned int i = get_global_id(0); 
    
    if(i>vertices_count)return;
    
    unsigned int particle_id=i/shape_vertices_count;
    unsigned int particle_pos_fist_component=particle_id*3;
    
    float3 particle_pos=(float3)(particles_position[particle_pos_fist_component],
                                particles_position[particle_pos_fist_component+1],
                                particles_position[particle_pos_fist_component+2]
    );
    
    if(i%shape_vertices_count==0){
        float3 gravity=(float3)(gravity_point[0],gravity_point[1],gravity_point[2]);
                
        float3 acceleration=normalize(gravity-particle_pos)*tpf;
        
        float3 velocity=(float3)(particles_velocity[particle_pos_fist_component],
                                particles_velocity[particle_pos_fist_component+1],
                                particles_velocity[particle_pos_fist_component+2]
        );
        velocity+=acceleration;
            
        particles_velocity[particle_pos_fist_component]=velocity.x;
        particles_velocity[particle_pos_fist_component+1]=velocity.y;
        particles_velocity[particle_pos_fist_component+2]=velocity.z;
        
        particles_position[particle_pos_fist_component]=particle_pos.x+velocity.x;
        particles_position[particle_pos_fist_component+1]=particle_pos.y+velocity.y;
        particles_position[particle_pos_fist_component+2]=particle_pos.z+velocity.z;
        
    }
    
    int shape_vertex_id=(i-particle_id*shape_vertices_count)*3;
    
    float3 shape_vertex_pos=(float3)(shape_vertices[shape_vertex_id],
                                    shape_vertices[shape_vertex_id+1],
                                    shape_vertices[shape_vertex_id+2]
    );
    
    
    int vid=i*3;
    vertices[vid]=particle_pos.x+shape_vertex_pos.x;
    vertices[vid+1]=particle_pos.y+shape_vertex_pos.y;
    vertices[vid+2]=particle_pos.z+shape_vertex_pos.z;  
}
