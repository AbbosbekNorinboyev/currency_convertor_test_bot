package uz.brb.test_bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.brb.test_bot.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);
}
