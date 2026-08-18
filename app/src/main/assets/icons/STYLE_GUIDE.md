# File Manager Icon System - Style Guide

## Grid System
- Canvas: 24 x 24
- ViewBox: `0 0 24 24`
- Padding: 2dp from all edges (effective area: 20x20)
- Grid alignment: 1px snap grid

## Design Style
- **Type**: Filled (Solid)
- **Corner Radius**: 2dp for small shapes, 3dp for container shapes
- **Stroke**: None (pure filled shapes)
- **Detail Level**: Minimal but recognizable
- **Visual Weight**: Consistent across all icons

## File Shape (Base)
- Width: 14dp, Height: 18dp
- Position: centered (x:5, y:3)
- Corner radius: 2dp
- Fold corner: 4dp triangle at top-right

## Folder Shape (Base)
- Width: 18dp, Height: 14dp
- Position: centered (x:3, y:6)
- Tab width: 6dp, Tab height: 3dp
- Corner radius: 2dp

## Color Strategy

### UI Icons (Monochrome / Tintable)
- Use `currentColor` or single fill
- Compatible with Light/Dark themes

### File Type Icons (Colored)
| Type       | Primary Color | Background |
|------------|---------------|------------|
| Image      | #4CAF50       | #E8F5E9    |
| Video      | #F44336       | #FFEBEE    |
| Audio      | #FF9800       | #FFF3E0    |
| Document   | #2196F3       | #E3F2FD    |
| PDF        | #E91E63       | #FCE4EC    |
| Text       | #607D8B       | #ECEFF1    |
| Code       | #795548       | #EFEBE9    |
| Archive    | #9C27B0       | #F3E5F5    |
| APK        | #00BCD4       | #E0F7FA    |
| Spreadsheet| #4CAF50       | #E8F5E9    |
| Presentation| #FF5722      | #FBE9E7    |

### Folder Indicator Colors
| Folder     | Indicator Color |
|------------|----------------|
| Download   | #2196F3        |
| Documents  | #607D8B        |
| Pictures   | #4CAF50        |
| Music      | #FF9800        |
| Movies     | #F44336        |
| DCIM       | #9C27B0        |
| Android    | #00BCD4        |

## Naming Convention
```
ic_[category]_[name].svg
```

Examples:
- `ic_file.svg`
- `ic_file_image.svg`
- `ic_folder.svg`
- `ic_folder_download.svg`
- `ic_action_delete.svg`
- `ic_nav_back.svg`
- `ic_status_error.svg`

## Stroke Rules (for outline variants)
- Stroke width: 1.5dp
- Line cap: round
- Line join: round
