module CocoapodsUtil
  require 'json'

  # 定义 Pod 模块和 Spec 类
  module Pod
    class Spec
      @@spec_data = {}

      def self.new(&block)
        @@spec_data.clear  # 清空之前的数据
        spec = self.allocate
        spec.instance_eval(&block) if block_given?
        @@spec_data
      end
      
      # 通用属性访问器
      def method_missing(method_name, *args)
        if method_name.to_s.end_with?('=')
          key = method_name.to_s.chomp('=')
          @@spec_data[key] = args.first
        else
          if args.empty?
            @@spec_data[method_name.to_s]
          else
            @@spec_data[method_name.to_s] = args.first
          end
        end
      end
    end
  end

  def self.convert_podspec_to_json(content)
    begin
      eval(content)
      Pod::Spec.class_variable_get(:@@spec_data).to_json
    rescue => e
      "{}"
    end
  end
end 