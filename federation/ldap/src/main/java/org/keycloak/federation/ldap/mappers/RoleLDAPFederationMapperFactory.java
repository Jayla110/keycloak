package org.keycloak.federation.ldap.mappers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.mappers.MapperConfigValidationException;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.UserFederationMapperSyncConfigRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleLDAPFederationMapperFactory extends AbstractLDAPFederationMapperFactory {

    private static final Logger logger = Logger.getLogger(RoleLDAPFederationMapperFactory.class);

    public static final String PROVIDER_ID = "role-ldap-mapper";

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty rolesDn = createConfigProperty(RoleLDAPFederationMapper.ROLES_DN, "LDAP Roles DN",
                "LDAP DN where are roles of this tree saved. For example 'ou=finance,dc=example,dc=org' ", ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(rolesDn);

        ProviderConfigProperty roleNameLDAPAttribute = createConfigProperty(RoleLDAPFederationMapper.ROLE_NAME_LDAP_ATTRIBUTE, "Role Name LDAP Attribute",
                "Name of LDAP attribute, which is used in role objects for name and RDN of role. Usually it will be 'cn' . In this case typical group/role object may have DN like 'cn=role1,ou=finance,dc=example,dc=org' ",
                ProviderConfigProperty.STRING_TYPE, LDAPConstants.CN);
        configProperties.add(roleNameLDAPAttribute);

        ProviderConfigProperty roleObjectClasses = createConfigProperty(RoleLDAPFederationMapper.ROLE_OBJECT_CLASSES, "Role Object Classes",
                "Object class (or classes) of the role object. It's divided by comma if more classes needed. In typical LDAP deployment it could be 'groupOfNames' . In Active Directory it's usually 'group' ",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(roleObjectClasses);

        ProviderConfigProperty membershipLDAPAttribute = createConfigProperty(RoleLDAPFederationMapper.MEMBERSHIP_LDAP_ATTRIBUTE, "Membership LDAP Attribute",
                "Name of LDAP attribute on role, which is used for membership mappings. Usually it will be 'member' ",
                ProviderConfigProperty.STRING_TYPE, LDAPConstants.MEMBER);
        configProperties.add(membershipLDAPAttribute);


        List<String> membershipTypes = new LinkedList<>();
        for (RoleLDAPFederationMapper.MembershipType membershipType : RoleLDAPFederationMapper.MembershipType.values()) {
            membershipTypes.add(membershipType.toString());
        }
        ProviderConfigProperty membershipType = createConfigProperty(RoleLDAPFederationMapper.MEMBERSHIP_ATTRIBUTE_TYPE, "Membership Attribute Type",
                "DN means that LDAP role has it's members declared in form of their full DN. For example 'member: uid=john,ou=users,dc=example,dc=com' . " +
                        "UID means that LDAP role has it's members declared in form of pure user uids. For example 'memberUid: john' .",
                ProviderConfigProperty.LIST_TYPE, membershipTypes);
        configProperties.add(membershipType);


        ProviderConfigProperty ldapFilter = createConfigProperty(RoleLDAPFederationMapper.ROLES_LDAP_FILTER,
                "LDAP Filter",
                "LDAP Filter adds additional custom filter to the whole query. Leave this empty if no additional filtering is needed. Otherwise make sure that filter starts with '(' and ends with ')'",
                ProviderConfigProperty.STRING_TYPE, null);
        configProperties.add(ldapFilter);


        List<String> modes = new LinkedList<>();
        for (RoleLDAPFederationMapper.Mode mode : RoleLDAPFederationMapper.Mode.values()) {
            modes.add(mode.toString());
        }
        ProviderConfigProperty mode = createConfigProperty(RoleLDAPFederationMapper.MODE, "Mode",
                "LDAP_ONLY means that all role mappings are retrieved from LDAP and saved into LDAP. READ_ONLY is Read-only LDAP mode where role mappings are " +
                        "retrieved from both LDAP and DB and merged together. New role grants are not saved to LDAP but to DB. IMPORT is Read-only LDAP mode where role mappings are retrieved from LDAP just at the time when user is imported from LDAP and then " +
                        "they are saved to local keycloak DB.",
                ProviderConfigProperty.LIST_TYPE, modes);
        configProperties.add(mode);


        List<String> roleRetrievers = new LinkedList<>();
        for (UserRolesRetrieveStrategy retriever : UserRolesRetrieveStrategy.values()) {
            roleRetrievers.add(retriever.toString());
        }
        ProviderConfigProperty retriever = createConfigProperty(RoleLDAPFederationMapper.USER_ROLES_RETRIEVE_STRATEGY, "User Roles Retrieve Strategy",
                "Specify how to retrieve roles of user. LOAD_ROLES_BY_MEMBER_ATTRIBUTE means that roles of user will be retrieved by sending LDAP query to retrieve all roles where 'member' is our user. " +
                        "GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE means that roles of user will be retrieved from 'memberOf' attribute of our user. " +
                        "LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY is applicable just in Active Directory and it means that roles of user will be retrieved recursively with usage of LDAP_MATCHING_RULE_IN_CHAIN Ldap extension."
                ,
                ProviderConfigProperty.LIST_TYPE, roleRetrievers);
        configProperties.add(retriever);


        ProviderConfigProperty useRealmRolesMappings = createConfigProperty(RoleLDAPFederationMapper.USE_REALM_ROLES_MAPPING, "Use Realm Roles Mapping",
                "If true, then LDAP role mappings will be mapped to realm role mappings in Keycloak. Otherwise it will be mapped to client role mappings", ProviderConfigProperty.BOOLEAN_TYPE, "true");
        configProperties.add(useRealmRolesMappings);

        ProviderConfigProperty clientIdProperty = createConfigProperty(RoleLDAPFederationMapper.CLIENT_ID, "Client ID",
                "Client ID of client to which LDAP role mappings will be mapped. Applicable just if 'Use Realm Roles Mapping' is false",
                ProviderConfigProperty.CLIENT_LIST_TYPE, null);
        configProperties.add(clientIdProperty);
    }

    @Override
    public String getHelpText() {
        return "Used to map role mappings of roles from some LDAP DN to Keycloak role mappings of either realm roles or client roles of particular client";
    }

    @Override
    public String getDisplayCategory() {
        return ROLE_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Role mappings";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public UserFederationMapperSyncConfigRepresentation getSyncConfig() {
        return new UserFederationMapperSyncConfigRepresentation(true, "sync-ldap-roles-to-keycloak", true, "sync-keycloak-roles-to-ldap");
    }

    @Override
    public void validateConfig(UserFederationMapperModel mapperModel) throws MapperConfigValidationException {
        checkMandatoryConfigAttribute(RoleLDAPFederationMapper.ROLES_DN, "LDAP Roles DN", mapperModel);
        checkMandatoryConfigAttribute(RoleLDAPFederationMapper.MODE, "Mode", mapperModel);

        String realmMappings = mapperModel.getConfig().get(RoleLDAPFederationMapper.USE_REALM_ROLES_MAPPING);
        boolean useRealmMappings = Boolean.parseBoolean(realmMappings);
        if (!useRealmMappings) {
            String clientId = mapperModel.getConfig().get(RoleLDAPFederationMapper.CLIENT_ID);
            if (clientId == null || clientId.trim().isEmpty()) {
                throw new MapperConfigValidationException("Client ID needs to be provided in config when Realm Roles Mapping is not used");
            }
        }

        String customLdapFilter = mapperModel.getConfig().get(RoleLDAPFederationMapper.ROLES_LDAP_FILTER);
        if ((customLdapFilter != null && customLdapFilter.trim().length() > 0) && (!customLdapFilter.startsWith("(") || !customLdapFilter.endsWith(")"))) {
            throw new MapperConfigValidationException("Custom Roles LDAP filter must starts with '(' and ends with ')'");
        }
    }

    @Override
    public UserFederationMapper create(KeycloakSession session) {
        return new RoleLDAPFederationMapper();
    }
}
