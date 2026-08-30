package com.tns.mes;

import com.tns.mes.basic.domain.MasterDataType;
import com.tns.mes.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MesApplicationTests {
    @Autowired
    private JwtService jwtService;

    @Test
    void contextLoadsAndSupportsThreeLanguages() {
        assertThat(jwtService).isNotNull();
        assertThat(MasterDataType.values()).hasSize(13);
        assertThat(MasterDataType.parse("production-line")).isEqualTo(MasterDataType.PRODUCTION_LINE);
    }
}

