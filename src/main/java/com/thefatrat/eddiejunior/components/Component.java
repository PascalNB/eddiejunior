package com.thefatrat.eddiejunior.components;

import com.thefatrat.eddiejunior.entities.Command;
import com.thefatrat.eddiejunior.entities.Interaction;
import com.thefatrat.eddiejunior.sources.Server;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;

public interface Component {

    /**
     * @return the id of the component
     */
    String getId();

    /**
     * Enables the component
     */
    void enable();

    /**
     * Disables the component
     */
    void disable();

    /**
     * @return whether the component is enabled
     */
    boolean isEnabled();

    /**
     * @return a list of the component's commands
     */
    List<Command> getCommands();

    /**
     * @return a list of the component's message context interactions
     */
    List<Interaction<Message>> getMessageInteractions();

    /**
     * @return a list of the component's user context interactions
     */
    List<Interaction<Member>> getMemberInteractions();

    /**
     * @return the current status of the component
     */
    String getStatus();

    /**
     * @return the server corresponding to the component
     */
    Server getServer();

}
