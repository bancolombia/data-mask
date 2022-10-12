package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.DataMaskingConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaskingFormat {
    private int leftVisible;
    private int rightVisible;
    private Boolean isEmail = false;
    private boolean queryOnly = true;
    private String format = DataMaskingConstants.ENCRYPTION_INLINE;

    public MaskingFormat(int leftVisible,int rightVisible){
        this.leftVisible = leftVisible;
        this.rightVisible = rightVisible;
    }

    public MaskingFormat(int leftVisible,int rightVisible, Boolean isEmail){
        this.leftVisible = leftVisible;
        this.rightVisible = rightVisible;
        this.isEmail = isEmail;
    }

    public MaskingFormat(int leftVisible,int rightVisible, Boolean isEmail, boolean queryOnly){
        this.leftVisible = leftVisible;
        this.rightVisible = rightVisible;
        this.isEmail = isEmail;
        this.queryOnly = queryOnly;
    }
}