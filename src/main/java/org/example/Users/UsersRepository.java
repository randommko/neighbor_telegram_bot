package org.example.Users;

import org.example.DataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.example.TablesDB.TG_USERS_TABLE;

public class UsersRepository {
    private final Logger logger = LoggerFactory.getLogger(UsersRepository.class);

    public void updateUserInDB(User user) {
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String updateUserQuery = "UPDATE " + TG_USERS_TABLE + " SET " +
                    "first_name = ?, " +
                    "is_bot = ?, " +
                    "last_name = ?, " +
                    "user_name = ?, " +
                    "language_code = ?, " +
                    "can_join_groups = ?, " +
                    "can_read_all_group_messages = ?, " +
                    "support_inline_queries = ?, " +
                    "is_premium = ?, " +
                    "added_to_attachment_menu = ? " +
                    "WHERE user_id = ?"; // Условие для обновления записи по user_id
            String userName = user.getUserName();
            if (userName != null)
                    userName = "@" + userName;

            try (PreparedStatement updateUser = connection.prepareStatement(updateUserQuery)) {
                updateUser.setString(1, user.getFirstName());
                updateUser.setBoolean(2, Boolean.TRUE.equals(user.getIsBot())); // Предотвращаем NullPointerException
                updateUser.setString(3, user.getLastName());
                updateUser.setString(4, userName);
                updateUser.setString(5, user.getLanguageCode());
                updateUser.setBoolean(6, Boolean.TRUE.equals(user.getCanJoinGroups())); // Замена null на false
                updateUser.setBoolean(7, Boolean.TRUE.equals(user.getCanReadAllGroupMessages()));
                updateUser.setBoolean(8, Boolean.TRUE.equals(user.getSupportInlineQueries()));
                updateUser.setBoolean(9, Boolean.TRUE.equals(user.getIsPremium()));
                updateUser.setBoolean(10, Boolean.TRUE.equals(user.getAddedToAttachmentMenu()));
                updateUser.setLong(11, user.getId()); // WHERE user_id = ?

                updateUser.executeUpdate(); // Используйте executeUpdate для DML-запросов

                int updatedRows = updateUser.executeUpdate(); // Возвращает число обновленных строк
                if (updatedRows == 0) {
                    logger.warn("Пользователь с user_id = {} не найден для обновления.", user.getId());
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при сохранении пользователя в БД: ", e);
        }
    }

    public void insertUserInDB(User user) {
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String insertUserQuery = "INSERT INTO " + TG_USERS_TABLE + " (" +
                    "user_id, " +
                    "first_name, " +
                    "is_bot, " +
                    "last_name, " +
                    "user_name, " +
                    "language_code, " +
                    "can_join_groups, " +
                    "can_read_all_group_messages, " +
                    "support_inline_queries, " +
                    "is_premium, " +
                    "added_to_attachment_menu  " +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertUser = connection.prepareStatement(insertUserQuery)) {
                insertUser.setLong(1, user.getId());
                insertUser.setString(2, user.getFirstName());
                insertUser.setBoolean(3, Boolean.TRUE.equals(user.getIsBot())); // Предотвращаем NullPointerException
                insertUser.setString(4, user.getLastName());
                insertUser.setString(5, "@" + user.getUserName());
                insertUser.setString(6, user.getLanguageCode());
                insertUser.setBoolean(7, Boolean.TRUE.equals(user.getCanJoinGroups())); // Замена null на false
                insertUser.setBoolean(8, Boolean.TRUE.equals(user.getCanReadAllGroupMessages()));
                insertUser.setBoolean(9, Boolean.TRUE.equals(user.getSupportInlineQueries()));
                insertUser.setBoolean(10, Boolean.TRUE.equals(user.getIsPremium()));
                insertUser.setBoolean(11, Boolean.TRUE.equals(user.getAddedToAttachmentMenu()));
                insertUser.executeUpdate(); // Используйте executeUpdate для DML-запросов
            }
        } catch (Exception e) {
            logger.error("Ошибка при сохранении пользователя в БД: ", e);
        }
    }

    public User getUserByID(Long userID) {
        try (Connection connection = DataSourceConfig.getDataSource().getConnection()) {
            String userNameQuery = "SELECT " +
                    "user_id, " +
                    "first_name, " +
                    "is_bot, " +
                    "last_name, " +
                    "user_name, " +
                    "language_code, " +
                    "can_join_groups, " +
                    "can_read_all_group_messages, " +
                    "support_inline_queries, " +
                    "is_premium, " +
                    "added_to_attachment_menu  " +
                    "FROM " + TG_USERS_TABLE +
                    " WHERE user_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(userNameQuery)) {
                checkStmt.setLong(1, userID);
                ResultSet resultSet = checkStmt.executeQuery();
                if (resultSet.next()) {
                    return new User(resultSet.getLong("user_id"),
                            resultSet.getString("first_name"),
                            resultSet.getBoolean("is_bot"),
                            resultSet.getString("last_name"),
                            resultSet.getString("user_name"),
                            resultSet.getString("language_code"),
                            resultSet.getBoolean("can_join_groups"),
                            resultSet.getBoolean("can_read_all_group_messages"),
                            resultSet.getBoolean("support_inline_queries"),
                            resultSet.getBoolean("is_premium"),
                            resultSet.getBoolean("added_to_attachment_menu")
                    );
                } else {
                    logger.warn("Запись не найдена для user_id: " + userID);
                    return null; // Возвращаем значение по умолчанию, если запись не найдена
                }

            }
        } catch (Exception e) {
            logger.error("Ошибка получения User из БД: ", e);
            return null;
        }
    }
}
