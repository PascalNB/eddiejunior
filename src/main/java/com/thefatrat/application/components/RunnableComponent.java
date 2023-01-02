package com.thefatrat.application.components;

import com.thefatrat.application.reply.Reply;

public interface RunnableComponent {

    void start(Reply reply);

    void stop(Reply reply);

    boolean isRunning();

    boolean isAutoRunnable();

}
