package com.example.postfactory2.Generate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.postfactory2.R;

import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<NewsItem> newsList;
    private OnNewsClickListener onNewsClickListener;

    public interface OnNewsClickListener {
        void onNewsClick(NewsItem newsItem);
    }

    public NewsAdapter(List<NewsItem> newsList, OnNewsClickListener onNewsClickListener) {
        this.newsList = newsList;
        this.onNewsClickListener = onNewsClickListener;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem newsItem = newsList.get(position);
        holder.bind(newsItem, onNewsClickListener);
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;
        private TextView tvPublicationDate;
        private TextView tvSource;
        private TextView tvLink;
        private View itemView;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPublicationDate = itemView.findViewById(R.id.tvPublicationDate);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvLink = itemView.findViewById(R.id.tvLink);
        }

        public void bind(NewsItem newsItem, OnNewsClickListener onNewsClickListener) {
            tvTitle.setText(newsItem.getTitle());
            tvPublicationDate.setText(newsItem.getPublicationDate());
            tvSource.setText(newsItem.getSource());
            tvLink.setText(newsItem.getLink());

            // Устанавливаем цвет текста в зависимости от наличия суммаризированного текста
            int textColor = newsItem.getSummarizedText() != null && 
                           !newsItem.getSummarizedText().isEmpty() && 
                           !newsItem.getSummarizedText().equals("Сгенерированный текст") 
                           ? itemView.getContext().getResources().getColor(R.color.primary)
                           : itemView.getContext().getResources().getColor(R.color.gray);
            
            tvTitle.setTextColor(textColor);

            // Верхняя часть карточки: заголовок, дата и источник
            itemView.setOnClickListener(v -> onNewsClickListener.onNewsClick(newsItem));

            // Нижняя часть карточки: ссылка
            tvLink.setOnClickListener(v -> {
                // Обработчик для открытия ссылки
                // Например, открыть в браузере:
                // Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.getLink()));
                // v.getContext().startActivity(intent);
            });
        }
    }
}
