package com.thefatrat.application;

import net.dv8tion.jda.api.entities.Message;

public record Command(String command, String[] args, Message event) {

}
