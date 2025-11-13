package com.ssafy.linkcare.point.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.point.entity.Point;
import com.ssafy.linkcare.point.repository.PointRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointRepository pointRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createPointForNewUser(User user) {
        Point point = new Point(user);
        pointRepository.save(point);
    }

    @Transactional
    public int addPoints(Long userPk, int amount) {
        if (amount <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "포인트는 0보다 커야 합니다.");
        }
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Point point = pointRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "포인트 정보를 찾을 수 없습니다."));

        point.add(amount);
        return point.getBalance();
    }

    @Transactional
    public void usePoints(Long userPk, int amount) {
        if (amount <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "포인트는 0보다 커야 합니다.");
        }
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Point point = pointRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "포인트 정보를 찾을 수 없습니다."));

        if (point.getBalance() < amount) {
            throw new CustomException(ErrorCode.INSUFFICIENT_POINTS);
        }
        point.use(amount);
    }

    public int getPointBalance(Long userPk) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Point point = pointRepository.findByUser(user)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "포인트 정보를 찾을 수 없습니다."));

        return point.getBalance();
    }
}
