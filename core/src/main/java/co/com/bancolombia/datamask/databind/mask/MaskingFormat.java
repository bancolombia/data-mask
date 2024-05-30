package co.com.bancolombia.datamask.databind.mask;

import co.com.bancolombia.datamask.DataMaskingConstants;
import co.com.bancolombia.datamask.databind.util.TransformationType;
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
    private Boolean isMultiMask = false;
    private String separator;
    private TransformationType transformationType = TransformationType.ONLY_MASK;
    private String format = DataMaskingConstants.ENCRYPTION_INLINE;

    public MaskingFormat(TransformationType transformationType){
        this.transformationType = transformationType;
    }

    public MaskingFormat(int leftVisible,int rightVisible){
        this.leftVisible = leftVisible;
        this.rightVisible = rightVisible;
    }

    public MaskingFormat(int leftVisible,int rightVisible, Boolean isEmail){
        this.leftVisible = leftVisible;
        this.rightVisible = rightVisible;
        this.isEmail = isEmail;
    }

    public MaskingFormat(int leftVisible,int rightVisible, Boolean isEmail, TransformationType transformationType){
        this.leftVisible = leftVisible;
        this.rightVisible = rightVisible;
        this.isEmail = isEmail;
        this.transformationType = transformationType;
    }

    public MaskingFormat(int leftVisible, int rightVisible, Boolean isEmail, Boolean isMultiMask, String separator, TransformationType transformationType) {
        this.leftVisible = leftVisible;
        this.rightVisible = rightVisible;
        this.isEmail = isEmail;
        this.isMultiMask = isMultiMask;
        this.transformationType = transformationType;
        this.separator = separator;
    }
}