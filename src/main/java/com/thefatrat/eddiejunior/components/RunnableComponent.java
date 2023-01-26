package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.reply.Reply;

public interface RunnableComponent {

    void start(Reply reply);

    void stop(Reply reply);

    boolean isRunning();

    boolean isAutoRunnable();

}
