package com.example.dungit.gallery.presentation.controller;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.dungit.gallery.R;
import com.example.dungit.gallery.presentation.Utils.ImageFileFilter;
import com.example.dungit.gallery.presentation.Utils.SortType;
import com.example.dungit.gallery.presentation.databasehelper.AlbumDatabaseHelper;
import com.example.dungit.gallery.presentation.entities.Album;
import com.example.dungit.gallery.presentation.entities.Photo;
import com.example.dungit.gallery.presentation.uis.adapters.AlbumAdapter;
import com.example.dungit.gallery.presentation.uis.adapters.SelectAlbumAdapter;
import com.example.dungit.gallery.presentation.uis.adapters.SelectHiddenAlbumAdapter;
import com.example.dungit.gallery.presentation.uis.dialog.ConfirmDialog;
import com.example.dungit.gallery.presentation.uis.dialog.InputDialog;

import java.io.File;
import java.util.LinkedList;


/**
 * Created by thaib on 17/04/2018.
 */

public class AlbumController {
    private static final File USER_ALBUM_FLODER
            = new File(Environment.getExternalStorageDirectory(), "Albums06/");
    private static final ImageFileFilter imageFileFilter = new ImageFileFilter();
    static {
        if (!USER_ALBUM_FLODER.exists())
            USER_ALBUM_FLODER.mkdirs();
    }

    private Context context;
    private AlbumAdapter albumAdapter;
    private AlbumDatabaseHelper databaseHelper;
    private LinkedList<Album> hiddenAlbums;

    public AlbumController(Context context, AlbumAdapter albumAdapter, LinkedList<Album> hiddenAlbums) {
        this.context = context;
        databaseHelper = new AlbumDatabaseHelper(context);
        this.albumAdapter = albumAdapter;
        this.hiddenAlbums= hiddenAlbums;
    }


    public void addNewAblbum() {
        InputDialog inputDialog = new InputDialog(context, "Nhập tên album mới",
                "Thêm", "Hủy", "New Album") {
            @Override
            public void onPositiveButtonClick(AlertDialog inputDialog, String output) {
                if (output.isEmpty()) {
                    Toast.makeText(context, "Tên album không được rỗng", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (output.startsWith(".")) {
                    Toast.makeText(context, "Tên album không được bắt đầu bằng dấu ."
                            , Toast.LENGTH_SHORT).show();
                    return;
                }
                if (albumAdapter.containsAlbum(output)) {
                    Toast.makeText(context, "Tên album đã tồn tại", Toast.LENGTH_SHORT).show();
                    return;
                }
                File newAlbumFile = new File(USER_ALBUM_FLODER, output);
                if (newAlbumFile.mkdirs()) {
                    Album album = new Album(newAlbumFile.hashCode(), output);
                    album.setFile(newAlbumFile);
                    albumAdapter.getAlbums().add(album);
                    albumAdapter.notifyDataSetChanged();
                    inputDialog.cancel();
                } else {
                    Toast.makeText(context, "Không thể tạo album " + output + " hãy chọn tên khác", Toast.LENGTH_SHORT).show();
                }

            }
        };
        inputDialog.showDialog();
    }

    public void deleteAlbum(final Album album) {
        ConfirmDialog confirmDialog = new ConfirmDialog(context,
                "Bạn có muốn xóa album: " + album.getName()
                , "Xóa", "hủy") {
            @Override
            public void onPositiveButtonClick() {
                if (album.isEmpty()) {
                    File file=album.getFile();
                    file.delete();
                } else {
                    String idStr = String.valueOf(album.getId());
                    context.getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            MediaStore.Images.ImageColumns.BUCKET_ID + " = ?",
                            new String[]{idStr});
                }
                albumAdapter.getAlbums().remove(album);
                albumAdapter.notifyDataSetChanged();
            }
        };
        confirmDialog.showDialog();
    }


    public void renameAlbum(final Album album) {
        InputDialog inputDialog = new InputDialog(context, "Nhập tên album:",
                "Đổi Tên", "Hủy", album.getName()) {
            @Override
            public void onPositiveButtonClick(AlertDialog inputDialog, String output) {
                if (output.isEmpty()) {
                    Toast.makeText(context, "Tên album không được rỗng", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (output.startsWith(".")) {
                    Toast.makeText(context, "Tên album không được bắt đầu bằng dấu ."
                            , Toast.LENGTH_SHORT).show();
                    return;
                }
                if (output.equals(album.getName())) {
                    inputDialog.cancel();
                    return;
                }
                if (albumAdapter.containsAlbum(output)) {
                    Toast.makeText(context, "Tên album đã tồn tại", Toast.LENGTH_SHORT).show();
                    return;
                }
                String name = databaseHelper.getBucketName(album.getId());
                if (name == null) {
                    databaseHelper.insertBucketName(album.getId(), output);
                } else {
                    databaseHelper.updateBucketName(album.getId(), output);
                }
                album.setName(output);
                albumAdapter.notifyDataSetChanged();
                inputDialog.cancel();
            }
        };
        inputDialog.showDialog();
    }

    public void hideAlbum(final Album album) {
        ConfirmDialog confirmDialog = new ConfirmDialog(context,
                "Bạn có muốn ẩn album: " + album.getName()
                , "Ẩn", "Hủy") {
            @Override
            public void onPositiveButtonClick() {
                databaseHelper.insertHideAlbum(album.getId());
                albumAdapter.getAlbums().remove(album);
                hiddenAlbums.add(album);
                albumAdapter.notifyDataSetChanged();
            }
        };
        confirmDialog.showDialog();
    }

    public void moveAlbum(final Album album) {
        if(album.isEmpty()){
            Toast.makeText(context,"Album trống",Toast.LENGTH_SHORT).show();
            return;
        }
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.select_album_dialog);
        View view = dialog.getWindow().getDecorView();

        final ListView listView = view.findViewById(R.id.listAlbumSelect);

        Button btnCancelMove = view.findViewById(R.id.btnCancelMove);
        btnCancelMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        SelectAlbumAdapter albumAdapter_ = new SelectAlbumAdapter(context, albumAdapter.getAlbums()) {
            @Override
            public void onRowClick(Album clickedAlbum) {
                File nAlbumFile = clickedAlbum.getFile();

                LinkedList<Photo> photos = album.getPhotos();

                for (Photo photo : photos) {
                    File filePhoto = photo.getFile();
                    File newFile = new File(nAlbumFile, filePhoto.getName());
                    if (filePhoto.renameTo(newFile)) {
                        Intent scanFileIntent = new Intent(
                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newFile));
                        context.sendBroadcast(scanFileIntent);
                        photo.setFile(newFile);
                        clickedAlbum.addPhoto(photo);
                    }
//                    File filePhoto = photo.getFile();
//                    File newFile = new File(nAlbumFile,filePhoto.getName());
//                    if(filePhoto.exists()) {
//                        try {
//                            FileChannel src = new FileInputStream(filePhoto).getChannel();
//                            FileChannel dst = new FileOutputStream(newFile).getChannel();
//                            dst.transferFrom(src, 0, src.size());
//                            Intent scanFileIntent = new Intent(
//                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newFile));
//                            context.sendBroadcast(scanFileIntent);
//                            photo.setFile(newFile);
//                            clickedAlbum.addPhoto(photo);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
                }
                context.getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.ImageColumns.BUCKET_ID + " = ?",
                        new String[]{String.valueOf(album.getId())});
                album.clearAlbum();
                albumAdapter.notifyDataSetChanged();
                dialog.cancel();
            }
        };
        listView.setAdapter(albumAdapter_);
        dialog.show();
    }

    public void unhideAlbum(){
        if(hiddenAlbums.size() <= 0){
            Toast.makeText(context,"Chưa có album nào bị ẩn",Toast.LENGTH_SHORT).show();
            return;
        }
        final Dialog dialog = new Dialog(context);
        final LinkedList<Album> unhideAlbums = new LinkedList<>();
        final LinkedList<Album> albums = albumAdapter.getAlbums();
        dialog.setContentView(R.layout.select_hidden_album_dialog);
        View view = dialog.getWindow().getDecorView();
        final ListView listView = view.findViewById(R.id.listHiddenAlbum);
        Button btnCancel = view.findViewById(R.id.btnCancelHideDialog);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        final LinkedList<Album> unhideAlbums_ = unhideAlbums;
        Button btnUnhid = view.findViewById(R.id.btnShowAlbum);
        btnUnhid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (Album unhideAlbum: unhideAlbums_) {
                    albums.add(unhideAlbum);
                    hiddenAlbums.remove(unhideAlbum);
                    databaseHelper.removeHideAlbum(unhideAlbum.getId());
                }
                albumAdapter.notifyDataSetChanged();
                dialog.cancel();
            }
        });
        SelectHiddenAlbumAdapter albumAdapter_ = new SelectHiddenAlbumAdapter(context
                ,hiddenAlbums,unhideAlbums) ;
        listView.setAdapter(albumAdapter_);
        dialog.show();




    }

    public void sortAlbums(SortType sortType) {
        albumAdapter.sortAlbum(sortType);

    }
}
