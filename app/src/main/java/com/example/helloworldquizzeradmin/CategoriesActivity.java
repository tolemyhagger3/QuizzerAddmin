package com.example.helloworldquizzeradmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class CategoriesActivity extends AppCompatActivity {

    private static final int GALLERY_REQUEST_CODE = 101;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    private RecyclerView recyclerView;
    private List<category_model> list;
    private Dialog loadingDialog, categoryDialog;
    /*
    For access inside category dialog views
     */
    private CircleImageView addImage;
    private EditText categoryName;
    private Button addBtn;

    private Uri uri;
    private StorageTask mUploadTask;
    //private String downloadURL;
    private category_adapter adapter;
    String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

          /*
        initial the loadingDialog
         */
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.roundded_corners));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);

        setCategoryDialog();
        /*
        For RecyclerView we need to a sample class and adapter and item list
         */

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Categories");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recyClerViewId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        list = new ArrayList<>();

        adapter = new category_adapter(list);
        recyclerView.setAdapter(adapter);

        loadingDialog.show();

        myRef.child("Categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    list.add(new category_model(dataSnapshot1.child("name").getValue().toString(),
                            Integer.parseInt(dataSnapshot1.child("sets").getValue().toString()),
                            dataSnapshot1.child("url").getValue().toString()
                    ));
                }
                adapter.notifyDataSetChanged();
                loadingDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CategoriesActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.add) {
            //show the dialog
            categoryDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setCategoryDialog() {
        categoryDialog = new Dialog(this);
        categoryDialog.setContentView(R.layout.add_category_dialog);
        categoryDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.roundded_box));
        categoryDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        categoryDialog.setCancelable(true);

        addImage = categoryDialog.findViewById(R.id.image);
        categoryName = categoryDialog.findViewById(R.id.categoryname);
        addBtn = categoryDialog.findViewById(R.id.add);

        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);

            }
        });

        addBtn.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                if (categoryName.getText().toString().isEmpty()) {
                    categoryName.setError("Category Name Required !");
                    return;
                }

                if (uri == null) {
                    Toast.makeText(CategoriesActivity.this, "please select a image", Toast.LENGTH_SHORT).show();
                    return;
                }
                categoryDialog.dismiss();
                //upload data
                uploadData();
            }


        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE
                && resultCode == RESULT_OK) {

            uri = data.getData();
            //Glide.with(getApplicationContext()).load(uri).into(addImage);
            addImage.setImageURI(uri);

        }

    }

    /*private void uploadImage(Bitmap bitmap) {
        loadingDialog.show();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();

        final StorageReference ref = storageReference.child("categories/").child(uri.getLastPathSegment() +".jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
        byte[] data = baos.toByteArray();

        final UploadTask uploadTask = ref.putBytes(data);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                loadingDialog.dismiss();
                Toast.makeText(CategoriesActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();

                uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return ref.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                             Uri downUri = task.getResult();
                             final String downloadURL = downUri.toString();
                            Map<String, Object> map = new HashMap<>();
                            map.put("name", categoryName.getText().toString());
                            map.put("sets", 0);
                            map.put("url", downloadURL);

                            final FirebaseDatabase database = FirebaseDatabase.getInstance();
                            database.getReference().child("Categories").child("category" + (list.size() + 1)).setValue(map)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                list.add(new category_model(categoryName.getText().toString(), 0, downloadURL));
                                                adapter.notifyDataSetChanged();
                                            } else {
                                                Toast.makeText(CategoriesActivity.this, "Dear Developer - Correction needed", Toast.LENGTH_SHORT).show();
                                            }
                                            loadingDialog.dismiss();
                                        }
                                    });
                            //uploadCategoryName();
                        } else {
                            loadingDialog.dismiss();
                            Toast.makeText(CategoriesActivity.this, "Hey man Correction needed", Toast.LENGTH_SHORT).show();
                        }
                    }

                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                loadingDialog.dismiss();
                Toast.makeText(CategoriesActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }*/
    private void uploadData() {
        loadingDialog.show();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        //getLastPathSegment() this method return image name
        final StorageReference imageReference = storageReference.child("categories/").child(uri.getLastPathSegment() + ".jpg");


        UploadTask uploadTask = imageReference.putFile(uri);
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return imageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            final String downloadURL = task.getResult().toString();
                            Map<String, Object> map = new HashMap<>();
                            map.put("name", categoryName.getText().toString());
                            map.put("sets", 0);
                            map.put("url", downloadURL);

                            final FirebaseDatabase database = FirebaseDatabase.getInstance();
                            database.getReference().child("Categories").child("category" + (list.size() + 1)).setValue(map)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                list.add(new category_model(categoryName.getText().toString(), 0, downloadURL));
                                                adapter.notifyDataSetChanged();
                                            } else {
                                                Toast.makeText(CategoriesActivity.this, "Dear Developer - Correction needed", Toast.LENGTH_SHORT).show();
                                            }
                                            loadingDialog.dismiss();
                                        }
                                    });
                            //uploadCategoryName();
                        } else {
                            loadingDialog.dismiss();
                            Toast.makeText(CategoriesActivity.this, "Hey man Correction needed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        /*.addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                    } else {
                        // Handle failures
                        Toast.makeText(CategoriesActivity.this, "Correction needed", Toast.LENGTH_SHORT).show();
                        loadingDialog.dismiss();
                    }
                }
            });
        }*/


    /*private void uploadCategoryName() {
            final String downloadURL = task.getResult().toString();

            //upload data into realtime database
            Map<String, Object> map = new HashMap<>();
            map.put("name", categoryName.getText().toString());
            map.put("sets", 0);
            map.put("url", downloadURL);

            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.getReference().child("Categories").child("category" + (list.size() + 1)).setValue(map)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                list.add(new category_model(categoryName.getText().toString(), 0, downloadURL));
                                adapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(CategoriesActivity.this, "Dear Developer - Correction needed", Toast.LENGTH_SHORT).show();
                            }
                            loadingDialog.dismiss();
                        }
                    });
        }

*/
    }

}