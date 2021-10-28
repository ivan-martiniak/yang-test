//package com.company;
//
//public class Main {
//    public static void main(String[] args) {
////        System.out.print("simple:website2");
////        System.out.print("module:website2");
////        System.out.print("simple:website2");
////        System.out.print("simple:website2");
////        System.out.print("simple:huhu");
////        System.out.print("simple:huhu");
////        System.out.print("yang:");
////        System.out.print("yang:");
////        System.out.print("yang:");
//    }
//}
package com.intellij.lang.yang.completion;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.yang.YangFileType;
import com.intellij.lang.yang.psi.YangFile;
import com.intellij.lang.yang.psi.YangTypes;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.TokenType;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.intellij.lang.yang.completion.YangCompletionContributorPopUp.POP_UP;

public class YangCompletionContributorBuilder implements FoldingBuilder {

    private PsiElement psiNode;

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
        psiNode = node.getPsi();

        var childrenOfCurrentFile = new ArrayList<>(PsiTreeUtil.findChildrenOfType(this.psiNode, PsiElement.class));

        setPrefixValues(childrenOfCurrentFile, POP_UP.getPrefixToYangModule(), YangTypes.YANG_PREFIX_KEYWORD);
        System.out.println(POP_UP.getPrefixToYangModule());
        setCompletionValues(childrenOfCurrentFile, POP_UP.getCurrentGroupingNames(), YangTypes.YANG_GROUPING_KEYWORD);
        setCompletionValues(childrenOfCurrentFile, POP_UP.getCurrentTypedefNames(), YangTypes.YANG_TYPEDEF_KEYWORD);
        setCompletionValues(childrenOfCurrentFile, POP_UP.getCurrentIdentityNames(), YangTypes.YANG_IDENTITY_KEYWORD);

        var prefixStrings = findAllPrefixStrings(childrenOfCurrentFile);

        updateCompletionOfPrefixes(prefixStrings, POP_UP.getImportedTypedefNames(), YangTypes.YANG_TYPEDEF_KEYWORD);
        updateCompletionOfPrefixes(prefixStrings, POP_UP.getImportedGroupingNames(), YangTypes.YANG_GROUPING_KEYWORD);
        updateCompletionOfPrefixes(prefixStrings, POP_UP.getImportedIdentityNames(), YangTypes.YANG_IDENTITY_KEYWORD);

        return new FoldingDescriptor[0];
        public static void main(String[] args) {
            System.out.println("simple:");
        }
    }

    @NotNull
    private List<String> findAllPrefixStrings(ArrayList<PsiElement> psiElements) {
        var prefixElements = new ArrayList<String>();
        POP_UP.getPrefixToYangModule().keySet().forEach(element -> psiElements.stream()
                .filter(psiElement -> isPrefixWithColon(element, psiElement))
                .map(psiElement -> psiElement.getNode().findChildByType(YangTypes.YANG_COLON))
                .filter(Objects::nonNull)
                .filter(this::isNextSiblingWhiteSpace)
                .forEach(astNode -> prefixElements.add(astNode.getPsi().getPrevSibling().getText())));
        return prefixElements;
    }

    @Nullable
    private PsiElement getPsiNode(Project project, String fileName) {
        Collection<VirtualFile> virtualFiles =
                FileTypeIndex.getFiles(YangFileType.INSTANCE, GlobalSearchScope.allScope(project));

        return virtualFiles.stream()
                .filter(virtualFile -> containsFileName(fileName, virtualFile))
                .map(virtualFile -> getYangFile(project, virtualFile))
                .findFirst().orElse(null);
    }

    private void setPrefixValues(@NotNull List<PsiElement> psiElements, Map<String, String> values, IElementType yangType) {
        updateValues(values);
        psiElements.stream()
                .filter(e -> isElementYangType(e, yangType)).collect(Collectors.toCollection(ArrayList::new))
                .forEach(e -> values.put(getCompletionValue(e).replace("\"", ""),
                        getValueForPrefixMap(e)));
    }

    private String getValueForPrefixMap(PsiElement e) {
        var prevSibling = e.getPrevSibling() == null ? null : e.getPrevSibling();
        while (prevSibling != null) {
            if (isElementYangType(prevSibling, YangTypes.YANG_IDENTIFIER)) {
                return prevSibling.getText();
            } else if (isElementYangType(prevSibling, YangTypes.YANG_IDENTIFIER)) {
                return "";
            }
            prevSibling = prevSibling.getPrevSibling();
        }
        return "";
    }
    private void updateCompletionOfPrefixes(List<String> prefixStrings,
                                            List<String> values, IElementType yangKeyword) {
        if (!prefixStrings.isEmpty()) {
            setCompletionValues(findChildrenInAnotherFile(prefixStrings), values, yangKeyword);
        }
    }

    @NotNull
    private List<PsiElement> findChildrenInAnotherFile(List<String> prefixes) {
        return new ArrayList<>(PsiTreeUtil.findChildrenOfType(
                getPsiNode(this.psiNode.getProject(),
                        POP_UP.getPrefixToYangModule().get(prefixes.get(0))),
                PsiElement.class));
    }

    private void setCompletionValues(@NotNull List<PsiElement> psiElements, List<String> values, IElementType yangType) {
        updateValues(values);
        psiElements.stream()
                .filter(e -> isElementYangType(e, yangType))
                .forEach(e -> values.add(getCompletionValue(e)));
    }

    private String getNonNullSibling(PsiElement e) {
        var psiWhiteSpace = nonNullElementAt(e.getTextOffset() + e.getTextLength());
        var nextElement = nonNullElementAt(psiWhiteSpace.getTextOffset() + psiWhiteSpace.getTextLength());
        return nextElement.getText();
    }

    private void updateValues(Map<String, String> values) {
        if (!values.isEmpty()) {
            values.clear();
        }
    }

    private void updateValues(List<String> values) {
        if (!values.isEmpty()) {
            values.clear();
        }
    }

    @NotNull
    private YangFile getYangFile(Project project, VirtualFile virtualFile) {
        return (YangFile) Objects.requireNonNull(PsiManager.getInstance(project).findFile(virtualFile));
    }

    private boolean containsFileName(String fileName, VirtualFile virtualFile) {
        return Objects.requireNonNull(virtualFile.getCanonicalPath()).contains(fileName);
    }

    private String getCompletionValue(PsiElement e) {
        return e.getNextSibling() == null ? getNonNullSibling(e) : e.getNextSibling().getNextSibling().getText();
    }

    private boolean isNextSiblingWhiteSpace(ASTNode astNode) {
        return astNode.getPsi().getNextSibling().getNode().getElementType().equals(TokenType.WHITE_SPACE);
    }

    @NotNull
    private PsiElement nonNullElementAt(int offset) {
        return Objects.requireNonNull(this.psiNode.findElementAt(offset));
    }

    private boolean isPrefixWithColon(String element, PsiElement psiElement) {
        return psiElement.getNode().getText().contains(element + ":");
    }

    private boolean isElementYangType(final PsiElement element, IElementType yangType) {
        return element.getNode().getElementType().equals(yangType);
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        return null;
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }
}
