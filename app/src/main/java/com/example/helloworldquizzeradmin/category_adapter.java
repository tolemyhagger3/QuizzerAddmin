package com.example.helloworldquizzeradmin;

import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class category_adapter extends RecyclerView.Adapter<category_adapter.viewHolder> {

    private List<category_model> category_modelList;

    public category_adapter(List<category_model> category_modelList) {
        this.category_modelList = category_modelList;
    }

    @NonNull
    @Override
    public category_adapter.viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_item,parent,false);
        return  new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull category_adapter.viewHolder holder, int position) {
        holder.setData(category_modelList.get(position).getUrl(),
                category_modelList.get(position).getName() , category_modelList.get(position).getSets());
    }

    @Override
    public int getItemCount() {
        return category_modelList.size();
    }

    class viewHolder extends RecyclerView.ViewHolder{
        private CircleImageView imageView;
        private TextView title;

        public viewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.circle_imageView);
            title = itemView.findViewById(R.id.category_title);
        }

        private void setData(String url , final String title , final int sets){
            //Glide.with(itemView.getContext()).load(url).centerCrop().into(imageView);
            Picasso.get().load(url).fit().into(imageView);
            this.title.setText(title);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent( imageView.getContext(),SetsActivity.class);
                    intent.putExtra("title",title);
                    intent.putExtra("sets",sets);
                    itemView.getContext().startActivity(intent);
                    //startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT))
                }
            });
        }
    }
}
