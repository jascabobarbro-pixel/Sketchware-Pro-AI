package pro.sketchware.fragments.ai;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import pro.sketchware.R;
import pro.sketchware.ai.AIChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AIChatAdapter extends RecyclerView.Adapter<AIChatAdapter.ViewHolder> {
    private final List<AIChatMessage> messages;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public AIChatAdapter(List<AIChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AIChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardMessage;
        private final TextView tvRole, tvContent, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMessage = itemView.findViewById(R.id.cardMessage);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(AIChatMessage message) {
            tvContent.setText(message.getContent());
            tvTime.setText(timeFormat.format(new Date(message.getTimestamp())));

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) cardMessage.getLayoutParams();

            switch (message.getRole()) {
                case USER:
                    tvRole.setText("You");
                    tvRole.setVisibility(View.GONE);
                    params.gravity = Gravity.END;
                    cardMessage.setCardBackgroundColor(
                            cardMessage.getContext().getColor(com.google.android.material.R.color.m3_sys_color_primary_light));
                    tvContent.setTextColor(
                            cardMessage.getContext().getColor(com.google.android.material.R.color.m3_sys_color_on_primary_light));
                    tvTime.setTextColor(
                            cardMessage.getContext().getColor(com.google.android.material.R.color.m3_sys_color_on_primary_light));
                    break;
                case ASSISTANT:
                    tvRole.setText("AI Assistant");
                    tvRole.setVisibility(View.VISIBLE);
                    params.gravity = Gravity.START;
                    cardMessage.setCardBackgroundColor(
                            cardMessage.getContext().getColor(com.google.android.material.R.color.m3_sys_color_surface_variant_light));
                    tvContent.setTextColor(
                            cardMessage.getContext().getColor(com.google.android.material.R.color.m3_sys_color_on_surface_variant_light));
                    tvTime.setTextColor(
                            cardMessage.getContext().getColor(com.google.android.material.R.color.m3_sys_color_on_surface_variant_light));
                    break;
                case SYSTEM:
                    tvRole.setText("System");
                    tvRole.setVisibility(View.VISIBLE);
                    params.gravity = Gravity.CENTER_HORIZONTAL;
                    cardMessage.setCardBackgroundColor(
                            cardMessage.getContext().getColor(com.google.android.material.R.color.m3_sys_color_tertiary_container_light));
                    tvContent.setTextColor(
                            cardMessage.getContext().getColor(com.google.android.material.R.color.m3_sys_color_on_tertiary_container_light));
                    break;
                case ERROR:
                    tvRole.setText("Error");
                    tvRole.setVisibility(View.VISIBLE);
                    params.gravity = Gravity.START;
                    cardMessage.setCardBackgroundColor(
                            cardMessage.getContext().getColor(com.google.android.material.R.color.m3_sys_color_error_container_light));
                    tvContent.setTextColor(
                            cardMessage.getContext().getColor(com.google.android.material.R.color.m3_sys_color_on_error_container_light));
                    break;
            }
            cardMessage.setLayoutParams(params);
        }
    }
}
