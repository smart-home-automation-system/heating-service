package cloud.cholewa.heating.home.service;

import cloud.cholewa.heating.model.Home;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final Home home;
}
