package akandan.bahou.kassy.serveur.dao;

import java.util.List;
import akandan.bahou.kassy.commun.dto.MessageChatDTO;
import akandan.bahou.kassy.commun.util.ExceptionPersistance;

public interface InterfaceMessageChatDAO {

    void sauvegarderMessage(MessageChatDTO messageDTO) throws ExceptionPersistance;

    List<MessageChatDTO> recupererMessagesParIdReunion(int idReunion) throws ExceptionPersistance;

    MessageChatDTO recupererMessageParId(long idMessage) throws ExceptionPersistance;

    boolean supprimerMessage(long idMessage) throws ExceptionPersistance;

    boolean mettreAJourMessage(MessageChatDTO messageDTO) throws ExceptionPersistance;
}