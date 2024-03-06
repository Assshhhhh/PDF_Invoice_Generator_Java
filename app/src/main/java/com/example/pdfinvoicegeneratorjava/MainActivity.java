package com.example.pdfinvoicegeneratorjava;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfDocument.PageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.pdfinvoicegeneratorjava.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends ComponentActivity {

    private ActivityMainBinding binding;

    private final int PERMISSION_REQUEST_CODE = 10;
    private static final int CREATE_FILE = 1;
    private final String TAG = "Tag";
    private int pdfHeight = 1080;
    private int pdfWidth = 720;
    private PdfDocument document;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        binding.createFromEditButton.setOnClickListener(view -> {
            if (checkPermissions()) {
                generatePDF(binding.editPdfContent.getText().toString());
            } else {
                requestPermission();
            }
        });

        binding.createFromViewButton.setOnClickListener(view -> {

            final Dialog invoiceDialog = new Dialog(this);
            invoiceDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            invoiceDialog.setContentView(R.layout.invoice_view);
            invoiceDialog.setCancelable(true);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(invoiceDialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            invoiceDialog.getWindow().setAttributes(lp);

            Button downloadInvoiceButton = invoiceDialog.findViewById(R.id.button_generate_invoice);
            invoiceDialog.show();

            downloadInvoiceButton.setOnClickListener(it -> {
                generatePDFFromView(invoiceDialog.findViewById(R.id.invoice_view));

            });

        });

    }

    private void generatePDF(String text) {
        document = new PdfDocument();
        Paint paintText = new Paint();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        paintText.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.NORMAL));
        paintText.setTextSize(25f);
        paintText.setColor(ContextCompat.getColor(this, R.color.black));
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("(PDF by Jamal Ali)", 396f, 50f, paintText);

        paintText.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        paintText.setColor(ContextCompat.getColor(this, R.color.grey));
        paintText.setTextSize(17f);
        paintText.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(text, 50f, 100f, paintText);

        document.finishPage(page);
        createFile();
    }

    private void generatePDFFromView(View view) {

        Bitmap bitmap = getBitmapFromView(view);
        document = new PdfDocument();
        PdfDocument.PageInfo myPageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page myPage = document.startPage(myPageInfo);
        Canvas canvas = myPage.getCanvas();
        canvas.drawBitmap(bitmap, 0, 0, null);
        document.finishPage(myPage);
        createFile();

    }

    private Bitmap getBitmapFromView(View view) {

        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null){
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        view.draw(canvas);
        return returnedBitmap;
    }

    private void createFile() {

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "new_pdf.pdf");
        startActivityForResult(intent, CREATE_FILE);

    }

    private void requestPermission() {
        // Implement your permission request logic here
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    private boolean checkPermissions() {
        // Implement your permission check logic here

        int permission1 = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int permission2 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);

        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED; // Placeholder value, replace with your actual logic
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK){
            Uri uri = null;
            if (data != null){
                uri = data.getData();

                if (document != null) {
                    ParcelFileDescriptor pfd = null;
                    try {

                        pfd = getContentResolver()
                                .openFileDescriptor(uri, "w");
                        FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                        document.writeTo(fileOutputStream);
                        document.close();
                        Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_SHORT).show();


                    } catch (IOException e){
                        try {
                            DocumentsContract.deleteDocument(getContentResolver(), uri);
                        } catch (FileNotFoundException ex){
                            ex.printStackTrace();
                        }
                        e.printStackTrace();
                    }
                }

            }
        }

    }
}
