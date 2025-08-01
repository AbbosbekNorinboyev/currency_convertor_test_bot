package uz.brb.test_bot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.brb.test_bot.repository.UserRepository;
import uz.brb.test_bot.service.UserService;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public Long count() {
        return userRepository.count();
    }

    @Override
    public Long activeCount() {
        return userRepository.findAll()
                .stream()
                .filter(filter -> filter.getLastActiveAt().toLocalDate()
                        .isAfter(LocalDate.now().minusDays(7))
                        || filter.getLastActiveAt().toLocalDate()
                        .isEqual(LocalDate.now().minusDays(7)))
                .count();
    }

    @Override
    public Long newUsers() {
        return userRepository.findAll()
                .stream()
                .filter(filter -> LocalDate.now().isEqual(filter.getLastActiveAt().toLocalDate()))
                .count();
    }
}
